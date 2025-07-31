package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialCreationOptions;
import app.cbo.oidc.java.server.webauthn.core.UserEntity;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom; 
import java.util.Base64; 
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebAuthnRegisterStartInteraction implements Interaction {

    private static final Logger LOGGER = Logger.getLogger(WebAuthnRegisterStartInteraction.class.getCanonicalName());
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final WebAuthnProtocolHandler webAuthnProtocolHandler;
    private final TemporaryChallengeStorage challengeStorage;
    private final SecureRandom secureRandom = new SecureRandom();

    // Configuration - these should ideally come from a central config
    private static final String RP_ID = "localhost"; 
    private static final String RP_NAME = "Example OIDC Server";

    public WebAuthnRegisterStartInteraction(
            WebAuthnProtocolHandler webAuthnProtocolHandler,
            TemporaryChallengeStorage challengeStorage) {
        this.webAuthnProtocolHandler = webAuthnProtocolHandler;
        this.challengeStorage = challengeStorage;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        String ongoingAuthId = null;
        String username = null;
        String displayName = null;

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, HttpCode.METHOD_NOT_ALLOWED, "{\"error\":\"Only GET requests are allowed.\"}");
                return;
            }

            Map<String, String> params = QueryStringParser.parse(exchange.getRequestURI().getQuery());
            username = params.get("username");
            displayName = params.get("displayName"); 
            ongoingAuthId = params.get("ongoing");

            if (username == null || username.trim().isEmpty()) {
                LOGGER.warning("Username is required for WebAuthn registration start.");
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Username is required.\"}");
                return;
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = username;
            }
            if (ongoingAuthId == null || ongoingAuthId.trim().isEmpty()) {
                LOGGER.warning("Ongoing transaction ID is required.");
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Ongoing transaction ID is required.\"}");
                return;
            }

            byte[] userHandle = new byte[16]; 
            secureRandom.nextBytes(userHandle);
            UserEntity userEntity = new UserEntity(userHandle, username, displayName, Optional.empty());

            PublicKeyCredentialCreationOptions options = webAuthnProtocolHandler.generateRegistrationOptions(
                    RP_ID, RP_NAME, userEntity, Collections.emptyList() /* excludeCredentials */
            );

            challengeStorage.storeChallengeData(ongoingAuthId, options.challenge(), userEntity, RP_ID);

            String jsonResponse = publicKeyCredentialCreationOptionsToJson(options);
            sendSuccessResponse(exchange, jsonResponse);
            LOGGER.info("WebAuthn registration start options sent for user: " + username);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during WebAuthn registration start", e);
            // Ensure the error message is also valid JSON or a simple string.
            // For consistency, making it a JSON string.
            String errorMsg = "{\"error\":\"An unexpected error occurred: " + e.getMessage().replace("\"", "'") + "\"}";
            sendErrorResponse(exchange, HttpCode.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    private String publicKeyCredentialCreationOptionsToJson(PublicKeyCredentialCreationOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        // rp: {id, name, icon?}
        sb.append("\"rp\":{");
        sb.append("\"id\":\"").append(options.rp().id()).append("\",");
        sb.append("\"name\":\"").append(options.rp().name()).append("\"");
        options.rp().icon().ifPresent(icon -> sb.append(",\"icon\":\"").append(icon).append("\""));
        sb.append("},");
        // user: {id, name, displayName, icon?}
        sb.append("\"user\":{");
        sb.append("\"id\":\"").append(B64URL_ENCODER.encodeToString(options.user().id())).append("\",");
        sb.append("\"name\":\"").append(options.user().name()).append("\",");
        sb.append("\"displayName\":\"").append(options.user().displayName()).append("\"");
        options.user().icon().ifPresent(icon -> sb.append(",\"icon\":\"").append(icon).append("\""));
        sb.append("},");
        // challenge
        sb.append("\"challenge\":\"").append(B64URL_ENCODER.encodeToString(options.challenge())).append("\",");
        // pubKeyCredParams: [{type, alg}]
        sb.append("\"pubKeyCredParams\":[");
        sb.append(options.pubKeyCredParams().stream()
                .map(p -> String.format("{\"type\":\"%s\",\"alg\":%d}", p.type(), p.alg()))
                .collect(Collectors.joining(",")));
        sb.append("]");
        // Optional fields
        options.timeout().ifPresent(t -> sb.append(",\"timeout\":").append(t));
        options.excludeCredentials().ifPresent(ecs -> {
            if (!ecs.isEmpty()) {
                sb.append(",\"excludeCredentials\":[");
                sb.append(ecs.stream()
                        .map(ec -> {
                            String transportsJson = "";
                            if (ec.transports().isPresent() && !ec.transports().get().isEmpty()){
                                transportsJson = ec.transports().get().stream()
                                    .map(t -> "\"" + t + "\"")
                                    .collect(Collectors.joining(",",",\"transports\":[","]"));
                            }
                            return String.format("{\"type\":\"%s\",\"id\":\"%s\"%s}",
                                ec.type(), B64URL_ENCODER.encodeToString(ec.id()), transportsJson);
                        })
                        .collect(Collectors.joining(",")));
                sb.append("]");
            }
        });
        options.authenticatorSelection().ifPresent(as -> {
            sb.append(",\"authenticatorSelection\":{");
            boolean firstSelectionField = true;
            if (as.authenticatorAttachment().isPresent()) {
                sb.append("\"authenticatorAttachment\":\"").append(as.authenticatorAttachment().get()).append("\"");
                firstSelectionField = false;
            }
            if (as.requireResidentKey().isPresent()) {
                if (!firstSelectionField) sb.append(",");
                sb.append("\"requireResidentKey\":").append(as.requireResidentKey().get());
                firstSelectionField = false;
            }
            if (as.residentKey().isPresent()) {
                if (!firstSelectionField) sb.append(",");
                sb.append("\"residentKey\":\"").append(as.residentKey().get()).append("\"");
                firstSelectionField = false;
            }
            if (as.userVerification().isPresent()) {
                if (!firstSelectionField) sb.append(",");
                sb.append("\"userVerification\":\"").append(as.userVerification().get()).append("\"");
            }
            sb.append("}");
        });
        options.attestation().ifPresent(a -> sb.append(",\"attestation\":\"").append(a).append("\""));
        // extensions (Map<String, Object>) is complex, skipping for this basic serializer

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
