package app.cbo.oidc.java.server.http.authenticate;

// Similar imports, plus AuthenticationSuccessfulInteraction, OngoingAuthsFinder, AuthorizeParams
import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.http.oidc.AuthenticationSuccessfulInteraction;
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
import java.util.Collections;
import java.util.Optional;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebAuthnLoginFinishInteractionTests {

    WebAuthnLoginFinishInteraction interaction;
    WebAuthnProtocolHandler mockProtocolHandler;
    TemporaryChallengeStorage mockChallengeStorage;
    CredentialRepository mockCredentialRepository;
    AuthenticationSuccessfulInteraction mockAuthSuccessInteraction;
    OngoingAuthsFinder mockOngoingAuthsFinder;
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
        mockAuthSuccessInteraction = mock(AuthenticationSuccessfulInteraction.class);
        mockOngoingAuthsFinder = mock(OngoingAuthsFinder.class);
        mockExchange = mock(HttpExchange.class);
        responseBodyStream = new ByteArrayOutputStream();
        requestHeaders = new Headers();
        responseHeaders = new Headers();

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(mockExchange.getResponseBody()).thenReturn(responseBodyStream);
        
        requestHeaders.add("Host", "localhost:8080");


        interaction = new WebAuthnLoginFinishInteraction(
            mockProtocolHandler, mockChallengeStorage, mockCredentialRepository, 
            mockAuthSuccessInteraction, mockOngoingAuthsFinder
        );
    }

    @Test
    void handle_validLogin_updatesCredentialAndReturnsSuccess() throws IOException, WebAuthnVerificationException {
        String ongoingIdStr = "loginFinish123";
        OngoingAuthId ongoingAuthId = new OngoingAuthId(ongoingIdStr);
        byte[] challengeBytes = "challenge".getBytes(StandardCharsets.UTF_8); // Raw challenge
        String rpId = "localhost";
        String credentialIdB64 = B64_URL_ENCODER.encodeToString("credIdRaw".getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/login/finish?ongoing=" + ongoingIdStr));
        when(mockChallengeStorage.retrieveChallenge(ongoingIdStr)).thenReturn(Optional.of(challengeBytes));
        when(mockChallengeStorage.retrieveRpId(ongoingIdStr)).thenReturn(Optional.of(rpId));

        // Client Data JSON construction
        String clientDataInnerChallengeB64 = B64_URL_ENCODER.encodeToString(challengeBytes);
        String clientDataJsonString = String.format("{\"type\":\"webauthn.get\",\"challenge\":\"%s\",\"origin\":\"http://localhost:8080\"}", clientDataInnerChallengeB64);
        String clientDataJsonForContainerB64 = B64_URL_ENCODER.encodeToString(clientDataJsonString.getBytes(StandardCharsets.UTF_8));
        
        String authenticatorDataB64 = B64_URL_ENCODER.encodeToString("authDataBytes".getBytes(StandardCharsets.UTF_8));
        String signatureB64 = B64_URL_ENCODER.encodeToString("sigBytes".getBytes(StandardCharsets.UTF_8));
        String requestJson = String.format(
            "{\"id\":\"%s\",\"rawId\":\"%s\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"%s\",\"authenticatorData\":\"%s\",\"signature\":\"%s\"}}",
            credentialIdB64, credentialIdB64, clientDataJsonForContainerB64, authenticatorDataB64, signatureB64
        ); 
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));
        
        WebAuthnCredential storedCredential = new WebAuthnCredential(UserId.of("testUser"), credentialIdB64, "pkCose", 0L, "userHandle", Collections.emptyList(), false, "none", "aaguid", "credName", 0L);
        when(mockCredentialRepository.findCredentialById(credentialIdB64)).thenReturn(Optional.of(storedCredential));
        
        // Capture PublicKeyCredentialContainer
        ArgumentCaptor<PublicKeyCredentialContainer> containerCaptor = ArgumentCaptor.forClass(PublicKeyCredentialContainer.class);

        when(mockProtocolHandler.verifyAuthentication(eq(challengeBytes), eq("http://localhost:8080"), eq(rpId), anyList(), containerCaptor.capture(), eq(storedCredential)))
            .thenReturn(storedCredential); 
            
        AuthorizeParams mockAuthorizeParams = mock(AuthorizeParams.class);
        when(mockAuthorizeParams.redirectUri()).thenReturn("http://client/redirect");
        when(mockOngoingAuthsFinder.find(ongoingAuthId)).thenReturn(Optional.of(mockAuthorizeParams));


        interaction.handle(mockExchange);

        // Assert captured PublicKeyCredentialContainer
        PublicKeyCredentialContainer capturedContainer = containerCaptor.getValue();
        assertEquals(credentialIdB64, capturedContainer.id());
        assertTrue(capturedContainer.response() instanceof AuthenticatorAssertionResponse);
        AuthenticatorAssertionResponse assertionResponse = (AuthenticatorAssertionResponse) capturedContainer.response();
        // clientDataJSON in AuthenticatorAssertionResponse is raw bytes of the JSON string
        assertArrayEquals(clientDataJsonString.getBytes(StandardCharsets.UTF_8), assertionResponse.clientDataJSON());
        assertArrayEquals(B64_URL_DECODER.decode(authenticatorDataB64), assertionResponse.authenticatorData());
        assertArrayEquals(B64_URL_DECODER.decode(signatureB64), assertionResponse.signature());


        verify(mockCredentialRepository).updateCredential(storedCredential);
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.OK.code()), anyLong());
        String jsonResp = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(jsonResp.contains("Login successful"), "Response was: "+jsonResp);
        assertTrue(jsonResp.contains("\"redirectTo\":\"http://client/redirect\""), "Response was: "+jsonResp);
    }

    @Test
    void handle_credentialNotFound_returnsUnauthorized() throws IOException, WebAuthnVerificationException {
        String ongoingIdStr = "loginCredNotFound123";
        byte[] challengeBytes = "challenge".getBytes(StandardCharsets.UTF_8);
        String rpId = "localhost";
        String credentialIdB64 = B64_URL_ENCODER.encodeToString("nonExistentCredId".getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/login/finish?ongoing=" + ongoingIdStr));
        when(mockChallengeStorage.retrieveChallenge(ongoingIdStr)).thenReturn(Optional.of(challengeBytes));
        when(mockChallengeStorage.retrieveRpId(ongoingIdStr)).thenReturn(Optional.of(rpId));

        // Minimal valid JSON to pass parsing
        String clientDataInnerChallengeB64 = B64_URL_ENCODER.encodeToString(challengeBytes);
        String clientDataJsonString = String.format("{\"type\":\"webauthn.get\",\"challenge\":\"%s\",\"origin\":\"http://localhost:8080\"}", clientDataInnerChallengeB64);
        String clientDataJsonForContainerB64 = B64_URL_ENCODER.encodeToString(clientDataJsonString.getBytes(StandardCharsets.UTF_8));
        String authenticatorDataB64 = B64_URL_ENCODER.encodeToString("authDataBytes".getBytes());
        String signatureB64 = B64_URL_ENCODER.encodeToString("sigBytes".getBytes());
        String requestJson = String.format(
            "{\"id\":\"%s\",\"rawId\":\"%s\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"%s\",\"authenticatorData\":\"%s\",\"signature\":\"%s\"}}",
            credentialIdB64, credentialIdB64, clientDataJsonForContainerB64, authenticatorDataB64, signatureB64
        );
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));

        when(mockCredentialRepository.findCredentialById(credentialIdB64)).thenReturn(Optional.empty());

        interaction.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(HttpCode.UNAUTHORIZED.code()), anyLong());
        String response = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("Credential not found"), "Response was: "+response);
    }
}
