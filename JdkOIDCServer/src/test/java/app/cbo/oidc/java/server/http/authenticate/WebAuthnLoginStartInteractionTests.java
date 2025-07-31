package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.core.*;
import app.cbo.oidc.java.server.webauthn.storage.CredentialRepository;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.List; //Ensure this is imported for anyList() with list argument

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebAuthnLoginStartInteractionTests {

    WebAuthnLoginStartInteraction interaction;
    WebAuthnProtocolHandler mockProtocolHandler;
    TemporaryChallengeStorage mockChallengeStorage;
    CredentialRepository mockCredentialRepository; 
    HttpExchange mockExchange;
    ByteArrayOutputStream responseBodyStream;
    Headers requestHeaders, responseHeaders;
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();


    @BeforeEach
    void setUp() throws IOException {
        mockProtocolHandler = mock(WebAuthnProtocolHandler.class);
        mockChallengeStorage = mock(TemporaryChallengeStorage.class);
        mockCredentialRepository = mock(CredentialRepository.class); 
        mockExchange = mock(HttpExchange.class);
        responseBodyStream = new ByteArrayOutputStream();
        requestHeaders = new Headers();
        responseHeaders = new Headers();

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(mockExchange.getResponseBody()).thenReturn(responseBodyStream);

        interaction = new WebAuthnLoginStartInteraction(mockProtocolHandler, mockChallengeStorage, mockCredentialRepository);
    }

    @Test
    void handle_validRequest_generatesOptionsAndStoresChallenge() throws IOException {
        String ongoingId = "ongoingLogin123";
        // Test case: No username provided initially, so allowCredentials should be Optional.empty()
        // as per the logic in WebAuthnLoginStartInteraction
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/login/start?ongoing=" + ongoingId));
        
        byte[] challenge = new byte[32]; 
        for(int i=0; i < challenge.length; i++) challenge[i] = (byte)i; // Predictable challenge

        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
            challenge, Optional.of(60000L), Optional.of("localhost"), 
            Optional.empty(), // Expect Optional.empty() when no username
            Optional.of("preferred"), Optional.empty()
        );
        // Adjust mock to expect Optional.empty() for allowedCredentials when no username
        when(mockProtocolHandler.generateAuthenticationOptions(eq("localhost"), eq(Optional.empty())))
            .thenReturn(options);

        interaction.handle(mockExchange);

        // Verify generateAuthenticationOptions was called with Optional.empty() for allowedCredentials
        verify(mockProtocolHandler).generateAuthenticationOptions(eq("localhost"), eq(Optional.empty()));
        verify(mockChallengeStorage).storeChallengeData(eq(ongoingId), eq(challenge), isNull(), eq("localhost")); 

        verify(mockExchange).sendResponseHeaders(eq(HttpCode.OK.code()), anyLong());
        assertEquals("application/json; charset=utf-8", responseHeaders.getFirst("Content-Type"));
        String jsonResponse = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(jsonResponse.contains("\"challenge\":\"" + B64URL_ENCODER.encodeToString(challenge) + "\""));
        assertTrue(jsonResponse.contains("\"rpId\":\"localhost\""));
        // Check that allowCredentials is NOT present in the JSON, as per Optional.empty()
        assertFalse(jsonResponse.contains("\"allowCredentials\""));
    }
    
    @Test
    void handle_validRequestWithUsername_usesEmptyListForAllowedCredentials() throws IOException {
        String ongoingId = "ongoingLoginWithUser123";
        String username = "loginUser";
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/login/start?ongoing=" + ongoingId + "&username=" + username));
        
        byte[] challenge = new byte[32]; 
        for(int i=0; i < challenge.length; i++) challenge[i] = (byte)(i+1); // Different challenge

        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
            challenge, Optional.of(60000L), Optional.of("localhost"), 
            Optional.of(Collections.emptyList()), // Expect empty list when username is present
            Optional.of("preferred"), Optional.empty()
        );
        // Adjust mock to expect Optional.of(Collections.emptyList()) for allowedCredentials
        when(mockProtocolHandler.generateAuthenticationOptions(eq("localhost"), eq(Optional.of(Collections.emptyList()))))
            .thenReturn(options);
        
        // The commented-out part for credentialRepository.findCredentialsByUsername is not active in current prod code
        // So, no need to mock it here unless that logic becomes active.

        interaction.handle(mockExchange);

        verify(mockProtocolHandler).generateAuthenticationOptions(eq("localhost"), eq(Optional.of(Collections.emptyList())));
        verify(mockChallengeStorage).storeChallengeData(eq(ongoingId), eq(challenge), isNull(), eq("localhost")); 

        verify(mockExchange).sendResponseHeaders(eq(HttpCode.OK.code()), anyLong());
        assertEquals("application/json; charset=utf-8", responseHeaders.getFirst("Content-Type"));
        String jsonResponse = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(jsonResponse.contains("\"challenge\":\"" + B64URL_ENCODER.encodeToString(challenge) + "\""));
        assertTrue(jsonResponse.contains("\"rpId\":\"localhost\""));
        // Check that allowCredentials IS present and is an empty array
        assertTrue(jsonResponse.contains("\"allowCredentials\":[]"));
    }
    
    @Test
    void handle_missingOngoingId_returnsBadRequest() throws IOException {
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/login/start"));
        interaction.handle(mockExchange);
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.BAD_REQUEST.code()), anyLong());
        assertTrue(responseBodyStream.toString(StandardCharsets.UTF_8).contains("Ongoing transaction ID is required"));
    }
}
