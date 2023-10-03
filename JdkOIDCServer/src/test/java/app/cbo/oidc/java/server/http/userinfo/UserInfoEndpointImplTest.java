package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserInfoEndpointImplTest {

    @Test
    void treatRequest() throws IOException {

        var tested = new UserInfoEndpointImpl(
                (userId, requestedScopes) -> Map.of("sub", "user", "family_name", "Smith"),
                accessToken -> new AccessTokenData(UserId.of("user"), Set.of("openid", "profile"))
        );

        var result = tested.treatRequest("accesstoken");
        assertThat(result)
                .isInstanceOf(UserInfoResponse.class);

        var req = TestHttpExchange.simpleGet();
        result.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(200);

        var content = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(content)
                .isNotEmpty();

        Map<String, Object> json = new ObjectMapper().reader().readValue(content, Map.class);
        assertThat(json)
                .isNotEmpty()
                .containsKey("sub")
                .containsKey("family_name");
        assertThat(json.get("sub"))
                .isEqualTo("user");
        assertThat(json.get("family_name"))
                .isEqualTo("Smith");
    }

    @Test
    void treatRequest_no_sub_in_claims() throws IOException {

        var tested = new UserInfoEndpointImpl(
                (userId, requestedScopes) -> Map.of("family_name", "Smith"),
                accessToken -> new AccessTokenData(UserId.of("user"), Set.of("openid", "profile"))
        );

        var result = tested.treatRequest("accesstoken");
        assertThat(result)
                .isInstanceOf(UserInfoResponse.class);

        var req = TestHttpExchange.simpleGet();
        result.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(200);

        var content = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(content)
                .isNotEmpty();

        Map<String, Object> json = new ObjectMapper().reader().readValue(content, Map.class);
        assertThat(json)
                .isNotEmpty()
                .containsKey("sub")
                .containsKey("family_name");
        assertThat(json.get("sub"))
                .isEqualTo("user");
        assertThat(json.get("family_name"))
                .isEqualTo("Smith");
    }

    @Test
    void invalid_access_token() throws IOException {

        var tested = new UserInfoEndpointImpl(
                (userId, requestedScopes) -> Map.of("family_name", "Smith"),
                accessToken -> {
                    throw new ForbiddenResponse(
                            HttpCode.FORBIDDEN, ForbiddenResponse.INVALID_TOKEN);
                }
        );

        var result = tested.treatRequest("accesstoken");
        assertThat(result)
                .isInstanceOf(ForbiddenResponse.class);

        var req = TestHttpExchange.simpleGet();
        result.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(403);
    }

    @Test
    void treatRequest_sub_differ() throws IOException {

        var tested = new UserInfoEndpointImpl(
                (userId, requestedScopes) -> Map.of(
                        "sub", "user",
                        "family_name", "Smith"),
                accessToken -> new AccessTokenData(
                        UserId.of("user_different"), //DIFFERENT !!
                        Set.of("openid", "profile"))
        );

        assertThatThrownBy(() -> tested.treatRequest("accesstoken"))
                .isInstanceOf(RuntimeException.class);
    }
}