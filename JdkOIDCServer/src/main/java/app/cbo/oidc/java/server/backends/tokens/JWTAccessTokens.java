package app.cbo.oidc.java.server.backends.tokens;

/**
 *
 * Metadata header : 'typ' should be 'at+JWT'
 *
 * Claims :
 * iss REQUIRED - as defined in Section 4.1.1 of [RFC7519].
 *
 * exp REQUIRED - as defined in Section 4.1.4 of [RFC7519].
 *
 * aud REQUIRED - as defined in Section 4.1.3 of [RFC7519]. See Section 3 for indications on how an authorization server should determine the value of "aud" depending on the request.
 *
 * sub REQUIRED - as defined in Section 4.1.2 of [RFC7519]. In cases of access tokens obtained through grants where a resource owner is involved, such as the authorization code grant, the value of "sub" SHOULD correspond to the subject identifier of the resource owner. In cases of access tokens obtained through grants where no resource owner is involved, such as the client credentials grant, the value of "sub" SHOULD correspond to an identifier the authorization server uses to indicate the client application. See Section 5 for more details on this scenario. Also, see Section 6 for a discussion about how different choices in assigning "sub" values can impact privacy.
 *
 * client_id REQUIRED - as defined in Section 4.3 of [RFC8693].
 *
 * iat REQUIRED - as defined in Section 4.1.6 of [RFC7519]. This claim identifies the time at which the JWT access token was issued.
 *
 * jti REQUIRED - as defined in Section 4.1.7 of [RFC7519].
 *
 * auth_time OPTIONAL - as defined in Section 2 of [OpenID.Core].
 * acr OPTIONAL - as defined in Section 2 of [OpenID.Core].
 * amr OPTIONAL - as defined in Section 2 of [OpenID.Core].
 */
public class JWTAccessTokens {
}
