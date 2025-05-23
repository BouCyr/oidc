package app.cbo.oidc.java.server.http.authenticate;

// Similar imports as above, plus CredentialRepository, WebAuthnVerificationException, WebAuthnCredential
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.WebAuthnVerificationException;
import app.cbo.oidc.java.server.webauthn.core.*;
import app.cbo.oidc.java.server.webauthn.storage.CredentialRepository;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Collections;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebAuthnRegisterFinishInteractionTests {

    WebAuthnRegisterFinishInteraction interaction;
    WebAuthnProtocolHandler mockProtocolHandler;
    TemporaryChallengeStorage mockChallengeStorage;
    CredentialRepository mockCredentialRepository;
    HttpExchange mockExchange;
    ByteArrayOutputStream responseBodyStream;
    Headers requestHeaders, responseHeaders;
    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();


    @BeforeEach
    void setUp() throws IOException {
        mockProtocolHandler = mock(WebAuthnProtocolHandler.class);
        mockChallengeStorage = mock(TemporaryChallengeStorage.class);
        mockCredentialRepository = mock(CredentialRepository.class);
        mockExchange = mock(HttpExchange.class);
        responseBodyStream = new ByteArrayOutputStream();
        requestHeaders = new Headers(); // For Host header
        responseHeaders = new Headers();

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(mockExchange.getResponseBody()).thenReturn(responseBodyStream);
        
        requestHeaders.add("Host", "localhost:8080"); // For expectedOrigin

        interaction = new WebAuthnRegisterFinishInteraction(mockProtocolHandler, mockChallengeStorage, mockCredentialRepository);
    }

    @Test
    void handle_validRegistration_savesCredentialAndReturnsSuccess() throws IOException, WebAuthnVerificationException {
        String ongoingId = "regFinish123";
        byte[] challengeBytes = "challenge".getBytes(StandardCharsets.UTF_8); // Raw challenge bytes
        UserEntity userEntity = new UserEntity(new byte[]{1}, "user", "User", Optional.empty());
        String rpId = "localhost";

        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/finish?ongoing=" + ongoingId));
        when(mockChallengeStorage.retrieveChallenge(ongoingId)).thenReturn(Optional.of(challengeBytes));
        when(mockChallengeStorage.retrieveUserEntity(ongoingId)).thenReturn(Optional.of(userEntity));
        when(mockChallengeStorage.retrieveRpId(ongoingId)).thenReturn(Optional.of(rpId));

        // Simulate incoming JSON data from client
        // Note: The clientDataJSON in the request body is the *actual JSON string*, not base64 encoded.
        // The challenge *within* that JSON string is base64url encoded.
        String clientDataInnerChallengeB64 = B64_URL_ENCODER.encodeToString(challengeBytes);
        String clientDataJsonString = String.format("{\"type\":\"webauthn.create\",\"challenge\":\"%s\",\"origin\":\"http://localhost:8080\"}", clientDataInnerChallengeB64);
        
        // The clientDataJSON field in the PublicKeyCredentialContainer's response part is Base64URL(clientDataJsonString)
        String clientDataJsonForContainerB64 = B64_URL_ENCODER.encodeToString(clientDataJsonString.getBytes(StandardCharsets.UTF_8));
        String attestationObjectB64 = B64_URL_ENCODER.encodeToString("attestationObjectBytes".getBytes(StandardCharsets.UTF_8));
        String rawCredIdB64 = B64_URL_ENCODER.encodeToString("rawCredId".getBytes(StandardCharsets.UTF_8));
        
        String requestJson = String.format(
            "{\"id\":\"%s\",\"rawId\":\"%s\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"%s\",\"attestationObject\":\"%s\"}}",
            rawCredIdB64, rawCredIdB64, clientDataJsonForContainerB64, attestationObjectB64
        );
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));
        
        WebAuthnCredential newCredential = new WebAuthnCredential(userEntity.id(), rawCredIdB64, "pk", 0L, B64_URL_ENCODER.encodeToString(userEntity.id()), Collections.emptyList(), false, "none", "aaguid", "name", System.currentTimeMillis());
        
        ArgumentCaptor<PublicKeyCredentialContainer> containerCaptor = ArgumentCaptor.forClass(PublicKeyCredentialContainer.class);

        when(mockProtocolHandler.verifyRegistration(eq(challengeBytes), eq("http://localhost:8080"), eq(rpId), containerCaptor.capture(), eq(userEntity)))
            .thenReturn(Optional.of(newCredential));

        interaction.handle(mockExchange);

        PublicKeyCredentialContainer capturedContainer = containerCaptor.getValue();
        assertEquals(rawCredIdB64, capturedContainer.id());
        assertArrayEquals(B64_URL_DECODER.decode(rawCredIdB64), capturedContainer.rawId()); 
        assertTrue(capturedContainer.response() instanceof AuthenticatorAttestationResponse);
        AuthenticatorAttestationResponse attestationResponse = (AuthenticatorAttestationResponse) capturedContainer.response();
        // clientDataJSON in AuthenticatorAttestationResponse is raw bytes of the JSON string
        assertArrayEquals(clientDataJsonString.getBytes(StandardCharsets.UTF_8), attestationResponse.clientDataJSON());
        assertArrayEquals(B64_URL_DECODER.decode(attestationObjectB64), attestationResponse.attestationObject());


        verify(mockCredentialRepository).saveCredential(newCredential);
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.OK.code()), anyLong());
        assertTrue(responseBodyStream.toString(StandardCharsets.UTF_8).contains("Registration successful"));
    }
    
    @Test
    void handle_protocolHandlerVerificationFails_returnsBadRequest() throws IOException, WebAuthnVerificationException {
        String ongoingId = "regFail123";
        byte[] challengeBytes = "challenge".getBytes(StandardCharsets.UTF_8);
        UserEntity userEntity = new UserEntity(new byte[]{1}, "user", "User", Optional.empty());
        String rpId = "localhost";

        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/finish?ongoing=" + ongoingId));
        when(mockChallengeStorage.retrieveChallenge(ongoingId)).thenReturn(Optional.of(challengeBytes));
        when(mockChallengeStorage.retrieveUserEntity(ongoingId)).thenReturn(Optional.of(userEntity));
        when(mockChallengeStorage.retrieveRpId(ongoingId)).thenReturn(Optional.of(rpId));
        
        //Minimal valid JSON structure to pass parsing
        String clientDataInnerChallengeB64 = B64_URL_ENCODER.encodeToString(challengeBytes);
        String clientDataJsonString = String.format("{\"type\":\"webauthn.create\",\"challenge\":\"%s\",\"origin\":\"http://localhost:8080\"}", clientDataInnerChallengeB64);
        String clientDataJsonForContainerB64 = B64_URL_ENCODER.encodeToString(clientDataJsonString.getBytes(StandardCharsets.UTF_8));
        String attestationObjectB64 = B64_URL_ENCODER.encodeToString("attestationObjectBytes".getBytes());
        String rawCredIdB64 = B64_URL_ENCODER.encodeToString("rawCredId".getBytes());
        String requestJson = String.format("{\"id\":\"%s\",\"rawId\":\"%s\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"%s\",\"attestationObject\":\"%s\"}}", rawCredIdB64,rawCredIdB64, clientDataJsonForContainerB64, attestationObjectB64);

        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));
        
        when(mockProtocolHandler.verifyRegistration(any(), anyString(), anyString(), any(PublicKeyCredentialContainer.class), any(UserEntity.class)))
            .thenThrow(new WebAuthnVerificationException("Test verification failure"));
            
        interaction.handle(mockExchange);
        
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.BAD_REQUEST.code()), anyLong());
        String response = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("Registration failed: Test verification failure"), "Response was: "+response);
    }

    @Test
    void handle_malformedClientJsonInRequest_returnsBadRequest() throws IOException {
        String ongoingId = "regMalformedJson123";
        when(mockChallengeStorage.retrieveChallenge(ongoingId)).thenReturn(Optional.of("challenge".getBytes()));
        when(mockChallengeStorage.retrieveUserEntity(ongoingId)).thenReturn(Optional.of(new UserEntity(new byte[1],"u","U",Optional.empty())));
        when(mockChallengeStorage.retrieveRpId(ongoingId)).thenReturn(Optional.of("localhost"));

        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/finish?ongoing=" + ongoingId));
        
        String requestJson = "{\"id\":\"credId\",\"rawId\":\"cmF3Q3JlZElk\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"Y2xpZW50RGF0YUpTT04\",\"attestationObject\":\"YXR0ZXN0YXRpb25PYmplY3Q\""; 
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));
            
        interaction.handle(mockExchange);
        
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.BAD_REQUEST.code()), anyLong());
        assertTrue(responseBodyStream.toString(StandardCharsets.UTF_8).contains("Invalid registration data"));
    }

    @Test
    void handle_missingHostHeader_returnsBadRequest() throws IOException {
        requestHeaders.remove("Host"); 
        String ongoingId = "regNoHost123";
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/finish?ongoing=" + ongoingId));
        
        interaction.handle(mockExchange);
        
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.BAD_REQUEST.code()), anyLong());
        assertTrue(responseBodyStream.toString(StandardCharsets.UTF_8).contains("Host header missing"));
    }
}
