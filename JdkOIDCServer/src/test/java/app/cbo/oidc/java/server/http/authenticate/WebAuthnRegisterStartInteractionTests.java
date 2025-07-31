package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.webauthn.WebAuthnProtocolHandler;
import app.cbo.oidc.java.server.webauthn.core.*;
import app.cbo.oidc.java.server.webauthn.storage.TemporaryChallengeStorage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebAuthnRegisterStartInteractionTests {

    WebAuthnRegisterStartInteraction interaction;
    WebAuthnProtocolHandler mockProtocolHandler;
    TemporaryChallengeStorage mockChallengeStorage;
    HttpExchange mockExchange;
    ByteArrayOutputStream responseBodyStream;
    Headers requestHeaders, responseHeaders;
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();


    @BeforeEach
    void setUp() throws IOException {
        mockProtocolHandler = mock(WebAuthnProtocolHandler.class);
        mockChallengeStorage = mock(TemporaryChallengeStorage.class);
        mockExchange = mock(HttpExchange.class);
        responseBodyStream = new ByteArrayOutputStream();
        requestHeaders = new Headers();
        responseHeaders = new Headers();

        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(mockExchange.getResponseBody()).thenReturn(responseBodyStream);

        interaction = new WebAuthnRegisterStartInteraction(mockProtocolHandler, mockChallengeStorage);
    }

    @Test
    void handle_validRequest_generatesOptionsAndStoresChallenge() throws IOException {
        // Arrange
        String ongoingId = "ongoingTest123";
        String username = "testuser";
        String displayName = "Test User";
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/start?ongoing=" + ongoingId + "&username=" + username + "&displayName=" + displayName));
        
        byte[] challenge = new byte[32]; 
        // Manually fill challenge for predictable encoding in assertion, if needed for exact match
        for(int i=0; i < challenge.length; i++) challenge[i] = (byte)i;


        UserEntity userEntity = new UserEntity(new byte[16], username, displayName, Optional.empty());
        PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
            new RpEntity("localhost", "Example OIDC Server", Optional.empty()), // Ensure RP name matches what's in interaction
            userEntity,
            challenge,
            Collections.singletonList(new PublicKeyCredentialParameter("public-key", -7)),
            Optional.of(60000L), Optional.empty(), 
            Optional.of(new AuthenticatorSelectionCriteria(Optional.of("platform"), Optional.empty(), Optional.empty(), Optional.of("required"))), 
            Optional.of("none"), Optional.empty()
        );
        when(mockProtocolHandler.generateRegistrationOptions(anyString(), anyString(), any(UserEntity.class), anyList()))
            .thenReturn(options);

        // Act
        interaction.handle(mockExchange);

        // Assert
        verify(mockProtocolHandler).generateRegistrationOptions(eq("localhost"), eq("Example OIDC Server"), any(UserEntity.class), eq(Collections.emptyList()));
        ArgumentCaptor<UserEntity> userEntityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(mockChallengeStorage).storeChallengeData(eq(ongoingId), eq(challenge), userEntityCaptor.capture(), eq("localhost"));
        assertEquals(username, userEntityCaptor.getValue().name());

        verify(mockExchange).sendResponseHeaders(eq(HttpCode.OK.code()), anyLong());
        // Corrected charset to utf-8 as per typical web standards (Original test had utf-f8)
        assertEquals("application/json; charset=utf-8", responseHeaders.getFirst("Content-Type")); 
        String jsonResponse = responseBodyStream.toString(StandardCharsets.UTF_8);
        assertTrue(jsonResponse.contains("\"challenge\":\"" + B64URL_ENCODER.encodeToString(challenge) + "\""));
        assertTrue(jsonResponse.contains("\"rp\":{\"id\":\"localhost\",\"name\":\"Example OIDC Server\"}"));
        assertTrue(jsonResponse.contains("\"authenticatorSelection\":{\"authenticatorAttachment\":\"platform\",\"userVerification\":\"required\"}"));
    }
    
    @Test
    void handle_missingUsername_returnsBadRequest() throws IOException {
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login/webauthn/register/start?ongoing=test"));
        interaction.handle(mockExchange);
        verify(mockExchange).sendResponseHeaders(eq(HttpCode.BAD_REQUEST.code()), anyLong());
        assertTrue(responseBodyStream.toString(StandardCharsets.UTF_8).contains("Username is required"));
    }
}
