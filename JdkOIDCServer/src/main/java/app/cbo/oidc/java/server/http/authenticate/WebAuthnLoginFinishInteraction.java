package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.Interaction;
// Assuming AuthenticationSuccessfulInteraction is already JDK-only or adapted
import app.cbo.oidc.java.server.http.oidc.AuthenticationSuccessfulInteraction; 
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.WebAuthnVerificationException;
import app.cbo.oidc.java.server.webauthn.core.*;
import app.cbo.oidc.java.server.webauthn.storage.CredentialRepository;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential;
//import app.cbo.oidc.java.server.datastored.user.UserId; // Not directly used in this version
import app.cbo.oidc.java.server.datastored.OngoingAuthId; 
import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder; 
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams; 


import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebAuthnLoginFinishInteraction implements Interaction {

    private static final Logger LOGGER = Logger.getLogger(WebAuthnLoginFinishInteraction.class.getCanonicalName());
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();

    private final WebAuthnProtocolHandler webAuthnProtocolHandler;
    private final TemporaryChallengeStorage challengeStorage;
    private final CredentialRepository credentialRepository;
    private final AuthenticationSuccessfulInteraction authenticationSuccessfulInteraction; // Kept for future, not directly used for response
    private final OngoingAuthsFinder ongoingAuthsFinder;

    public WebAuthnLoginFinishInteraction(
            WebAuthnProtocolHandler webAuthnProtocolHandler,
            TemporaryChallengeStorage challengeStorage,
            CredentialRepository credentialRepository,
            AuthenticationSuccessfulInteraction authenticationSuccessfulInteraction,
            OngoingAuthsFinder ongoingAuthsFinder) {
        this.webAuthnProtocolHandler = webAuthnProtocolHandler;
        this.challengeStorage = challengeStorage;
        this.credentialRepository = credentialRepository;
        this.authenticationSuccessfulInteraction = authenticationSuccessfulInteraction;
        this.ongoingAuthsFinder = ongoingAuthsFinder;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        String ongoingAuthIdStr = null;
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, HttpCode.METHOD_NOT_ALLOWED, "{\"error\":\"Only POST requests are allowed.\"}");
                return;
            }

            Map<String, String> params = QueryStringParser.parse(exchange.getRequestURI().getQuery());
            ongoingAuthIdStr = params.get("ongoing");

            if (ongoingAuthIdStr == null || ongoingAuthIdStr.trim().isEmpty()) {
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Ongoing transaction ID is required.\"}");
                return;
            }
            
            OngoingAuthId ongoingAuthId = new OngoingAuthId(ongoingAuthIdStr);

            Optional<byte[]> storedChallengeOpt = challengeStorage.retrieveChallenge(ongoingAuthIdStr);
            Optional<String> rpIdOpt = challengeStorage.retrieveRpId(ongoingAuthIdStr);


            if (storedChallengeOpt.isEmpty() || rpIdOpt.isEmpty()) {
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Login session expired or invalid.\"}");
                return;
            }
            
            byte[] storedChallenge = storedChallengeOpt.get();
            String expectedRpId = rpIdOpt.get();
            
            String hostHeader = exchange.getRequestHeaders().getFirst("Host");
            if (hostHeader == null || hostHeader.trim().isEmpty()) {
                LOGGER.warning("Host header is missing, cannot determine expected origin securely.");
                sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Host header missing.\"}");
                return;
            }
            String expectedOrigin = "http://" + hostHeader; 

            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            PublicKeyCredentialContainer assertionContainer = parsePublicKeyCredentialContainer(requestBody, false);

            Optional<WebAuthnCredential> storedCredentialOpt = credentialRepository.findCredentialById(assertionContainer.id());
            if (storedCredentialOpt.isEmpty()) {
                throw new WebAuthnVerificationException("Credential not found for ID: " + assertionContainer.id());
            }
            WebAuthnCredential storedCredential = storedCredentialOpt.get();
            
            // Construct allowCredentials list based on the specific credential being asserted.
            // This is sufficient for verification as the client has already selected a credential.
            List<PublicKeyCredentialDescriptor> allowCredentials = List.of(
                new PublicKeyCredentialDescriptor("public-key", B64URL_DECODER.decode(storedCredential.getCredentialId()), 
                    Optional.ofNullable(storedCredential.getTransports()).filter(t -> !t.isEmpty()))
            );

            WebAuthnCredential updatedCredential = webAuthnProtocolHandler.verifyAuthentication(
                    storedChallenge, expectedOrigin, expectedRpId, allowCredentials, assertionContainer, storedCredential
            );

            credentialRepository.updateCredential(updatedCredential); 
            LOGGER.info("WebAuthn login successful for user associated with credentialId: " + updatedCredential.getCredentialId());

            AuthorizeParams originalAuthorizeParams = ongoingAuthsFinder.find(ongoingAuthId)
                    .orElseThrow(() -> new WebAuthnVerificationException("Unable to retrieve original authorization request for " + ongoingAuthIdStr));
            
            String redirectTo = originalAuthorizeParams.redirectUri() != null ? originalAuthorizeParams.redirectUri() : "/default-redirect"; 
            String successJson = String.format("{\"status\":\"success\",\"message\":\"Login successful.\", \"redirectTo\":\"%s\"}", escapeJsonValue(redirectTo));
            sendSuccessResponse(exchange, successJson);


        } catch (WebAuthnVerificationException e) {
            LOGGER.log(Level.WARNING, "WebAuthn login verification failed for ongoingAuthId " + ongoingAuthIdStr, e);
            sendErrorResponse(exchange, HttpCode.UNAUTHORIZED, "{\"error\":\"Login failed: " + escapeJsonValue(e.getMessage()) + "\"}");
        } catch (JsonParsingException e) {
            LOGGER.log(Level.WARNING, "Failed to parse login response JSON for ongoingAuthId " + ongoingAuthIdStr, e);
            sendErrorResponse(exchange, HttpCode.BAD_REQUEST, "{\"error\":\"Invalid login data: " + escapeJsonValue(e.getMessage()) + "\"}");
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during WebAuthn login finish for ongoingAuthId " + ongoingAuthIdStr, e);
            sendErrorResponse(exchange, HttpCode.INTERNAL_SERVER_ERROR, "{\"error\":\"An unexpected error occurred: " + escapeJsonValue(e.getMessage()) + "\"}");
        }
    }
    
    private String escapeJsonValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Simplified JSON parser for PublicKeyCredentialContainer
    protected PublicKeyCredentialContainer parsePublicKeyCredentialContainer(String json, boolean isRegistration) throws JsonParsingException {
        try {
            String id = extractJsonStringValue(json, "id");
            String rawIdB64 = extractJsonStringValue(json, "rawId");
            String type = extractJsonStringValue(json, "type");

            if (id == null || rawIdB64 == null || type == null) {
                throw new JsonParsingException("Missing one or more required fields: id, rawId, type", null);
            }
            byte[] rawId = B64URL_DECODER.decode(rawIdB64);

            String responseJson = extractJsonObjectString(json, "response");
            if (responseJson == null) {
                 throw new JsonParsingException("Missing 'response' object in JSON", null);
            }
            
            AuthenticatorAttestationResponse attestationResponse = null;
            AuthenticatorAssertionResponse assertionResponse = null;

            String clientDataJsonB64 = extractJsonStringValue(responseJson, "clientDataJSON");
             if (clientDataJsonB64 == null) {
                throw new JsonParsingException("Missing 'clientDataJSON' in response object", null);
            }

            if (isRegistration) {
                String attestationObjectB64 = extractJsonStringValue(responseJson, "attestationObject");
                if (attestationObjectB64 == null) {
                    throw new JsonParsingException("Missing 'attestationObject' in registration response object", null);
                }
                attestationResponse = new AuthenticatorAttestationResponse(
                        B64URL_DECODER.decode(clientDataJsonB64),
                        B64URL_DECODER.decode(attestationObjectB64)
                );
            } else {
                String authenticatorDataB64 = extractJsonStringValue(responseJson, "authenticatorData");
                String signatureB64 = extractJsonStringValue(responseJson, "signature");
                 if (authenticatorDataB64 == null || signatureB64 == null) {
                    throw new JsonParsingException("Missing 'authenticatorData' or 'signature' in assertion response object", null);
                }
                String userHandleB64 = extractJsonStringValue(responseJson, "userHandle"); // Optional
                
                assertionResponse = new AuthenticatorAssertionResponse(
                        B64URL_DECODER.decode(clientDataJsonB64),
                        B64URL_DECODER.decode(authenticatorDataB64),
                        B64URL_DECODER.decode(signatureB64),
                        userHandleB64 != null ? Optional.of(B64URL_DECODER.decode(userHandleB64)) : Optional.empty()
                );
            }
            
            return new PublicKeyCredentialContainer(id, rawId, 
                isRegistration ? attestationResponse : assertionResponse, 
                Optional.empty(), // authenticatorAttachment (not parsed by this simple parser)
                Collections.emptyMap(), // clientExtensionResults (not parsed by this simple parser)
                type);

        } catch (IllegalArgumentException e) { // Catch Base64 decoding errors
            throw new JsonParsingException("Failed to decode Base64URL string: " + e.getMessage(), e);
        } catch (Exception e) { // Catch other parsing related issues
            if (e instanceof JsonParsingException) throw (JsonParsingException)e;
            throw new JsonParsingException("Failed to parse PublicKeyCredentialContainer JSON: " + e.getMessage(), e);
        }
    }

    // Using regex for slightly more robust (but still naive) extraction.
    protected String extractJsonStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; 
    }
    
    protected String extractJsonObjectString(String json, String key) {
        String searchKey = "\"" + key + "\":{";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            //try without quotes for key if it's the outermost object (though not standard for nested)
             searchKey = key + ":{";
             keyIndex = json.indexOf(searchKey);
             if (keyIndex == -1) return null;
        }

        int objectStart = json.indexOf('{', keyIndex);
        if (objectStart == -1) return null;

        int braceCount = 0;
        int objectEnd = -1;
        for (int i = objectStart; i < json.length(); i++) {
            if (json.charAt(i) == '{') braceCount++;
            else if (json.charAt(i) == '}') braceCount--;
            if (braceCount == 0) {
                objectEnd = i + 1;
                break;
            }
        }
        if (objectEnd == -1) return null;
        return json.substring(objectStart, objectEnd);
    }
    
    protected static class JsonParsingException extends IOException { //Made protected static from RegisterFinish
        public JsonParsingException(String message, Throwable cause) { super(message, cause); }
    }

    private void sendSuccessResponse(HttpExchange exchange, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
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
