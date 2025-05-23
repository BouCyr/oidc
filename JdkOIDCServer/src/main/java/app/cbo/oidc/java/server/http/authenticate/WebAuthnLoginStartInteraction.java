package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialRequestOptions;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialDescriptor;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;
import app.cbo.oidc.java.server.webauthn.storage.CredentialRepository; // Assuming this interface exists

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebAuthnLoginStartInteraction implements Interaction {

    private static final Logger LOGGER = Logger.getLogger(WebAuthnLoginStartInteraction.class.getCanonicalName());
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final WebAuthnProtocolHandler webAuthnProtocolHandler;
    private final TemporaryChallengeStorage challengeStorage;
    private final CredentialRepository credentialRepository; // To fetch allowed credentials by username

    // Configuration
    private static final String RP_ID = "localhost";

    public WebAuthnLoginStartInteraction(
            WebAuthnProtocolHandler webAuthnProtocolHandler,
            TemporaryChallengeStorage challengeStorage,
            CredentialRepository credentialRepository) { // Added CredentialRepository
        this.webAuthnProtocolHandler = webAuthnProtocolHandler;
        this.challengeStorage = challengeStorage;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        String ongoingAuthId = null;
        String username = null; 

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, HttpCode.METHOD_NOT_ALLOWED, "{\"error\":\"Only GET requests are allowed.\"}");
                return;
            }

            Map<String, String> params = QueryStringParser.parse(exchange.getRequestURI().getQuery());
            ongoingAuthId = params.get("ongoing");
            username = params.get("username"); 

            if (ongoingAuthId == null || ongoingAuthId.trim().isEmpty()) {
                LOGGER.warning("Ongoing transaction ID is required.");
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Ongoing transaction ID is required.\"}");
                return;
            }

            Optional<List<PublicKeyCredentialDescriptor>> allowedCredentials;
            if (username != null && !username.trim().isEmpty()) {
                // The presence of a username might imply that we *could* list specific credentials.
                // However, to keep it simple and often preferred for a better UX (allow any credential registered for this RP),
                // or to support discoverable credentials (resident keys), we can pass an empty list.
                // An empty list tells the authenticator it *can* use a specific credential if it has one for this RP ID,
                // but doesn't restrict it to only those listed.
                // If the list were non-empty, it would typically restrict the choices.
                // For discoverable credentials, the username might not even be needed by the client.
                // The commented-out code below shows how one *might* fetch and map them if desired.
                /*
                List<app.cbo.oidc.java.server.datastored.user.WebAuthnCredential> userCreds = credentialRepository.findCredentialsByUsername(username); // Assuming method exists
                List<PublicKeyCredentialDescriptor> descriptors = userCreds.stream()
                   .map(uc -> {
                       try {
                           // Assuming uc.getCredentialId() is Base64URL string of the raw ID
                           byte[] rawId = Base64.getUrlDecoder().decode(uc.getCredentialId());
                           return new PublicKeyCredentialDescriptor("public-key", rawId, 
                               Optional.ofNullable(uc.getTransports()).filter(t -> !t.isEmpty()));
                       } catch (IllegalArgumentException e) {
                           LOGGER.log(Level.WARNING, "Error decoding credential ID for user " + username, e);
                           return null;
                       }
                   })
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
                allowedCredentials = Optional.of(descriptors);
                */
                allowedCredentials = Optional.of(Collections.emptyList());
            } else {
                // If no username is provided, we rely on discoverable credentials (client-side resident keys).
                // In this case, allowCredentials should typically be omitted or empty.
                allowedCredentials = Optional.empty(); 
            }


            PublicKeyCredentialRequestOptions options = webAuthnProtocolHandler.generateAuthenticationOptions(
                    RP_ID,
                    allowedCredentials
            );

            // Store challenge for the finish step. UserEntity is not stored for login start.
            // RP_ID is stored to be verified at finish.
            challengeStorage.storeChallengeData(ongoingAuthId, options.challenge(), null, RP_ID);
            
            String jsonResponse = publicKeyCredentialRequestOptionsToJson(options);
            sendSuccessResponse(exchange, jsonResponse);
            LOGGER.info("WebAuthn login start options sent. Username hint: " + (username != null ? username : "none"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during WebAuthn login start", e);
            String errorMsg = "{\"error\":\"An unexpected error occurred: " + e.getMessage().replace("\"", "'") + "\"}";
            sendErrorResponse(exchange, HttpCode.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    private String publicKeyCredentialRequestOptionsToJson(PublicKeyCredentialRequestOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"challenge\":\"").append(B64URL_ENCODER.encodeToString(options.challenge())).append("\"");
        options.rpId().ifPresent(r -> sb.append(",\"rpId\":\"").append(r).append("\""));
        options.timeout().ifPresent(t -> sb.append(",\"timeout\":").append(t));
        options.userVerification().ifPresent(uv -> sb.append(",\"userVerification\":\"").append(uv).append("\""));
        
        options.allowCredentials().ifPresent(acs -> {
            // Even if the list is empty, if `allowCredentials` itself is present, send an empty array.
            // The WebAuthn spec implies that an empty allowCredentials array is valid and means
            // "let the client choose from any credentials associated with the rpId".
            sb.append(",\"allowCredentials\":[");
            if (!acs.isEmpty()) {
                sb.append(acs.stream()
                        .map(ac -> {
                            StringBuilder transportJson = new StringBuilder();
                            ac.transports().ifPresent(transports -> {
                                if (!transports.isEmpty()) {
                                    transportJson.append(",\"transports\":[");
                                    transportJson.append(transports.stream()
                                            .map(t -> "\"" + t + "\"")
                                            .collect(Collectors.joining(",")));
                                    transportJson.append("]");
                                }
                            });
                            return String.format("{\"type\":\"%s\",\"id\":\"%s\"%s}",
                                    ac.type(), B64URL_ENCODER.encodeToString(ac.id()), transportJson.toString());
                        })
                        .collect(Collectors.joining(",")));
            }
            sb.append("]");
        });
        // extensions is complex, skipping for this basic serializer
        sb.append("}");
        return sb.toString();
    }

    private void sendSuccessResponse(HttpExchange exchange, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); // Corrected charset
        exchange.sendResponseHeaders(HttpCode.OK.code(), jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorResponse(HttpExchange exchange, HttpCode code, String errorJson) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code.code(), errorJson.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes(StandardCharsets.UTF_8));
        }
    }
}
