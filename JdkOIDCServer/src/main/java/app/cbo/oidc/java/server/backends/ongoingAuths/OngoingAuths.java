package app.cbo.oidc.java.server.backends.ongoingAuths;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


/**
 * Provides a way to store authentication info on the server when doing user interaction. This is not a part of OAUTH/OIDC.
 */
@Injectable
public class OngoingAuths implements OngoingAuthsFinder, OngoingAuthsStorer {


    public OngoingAuths() {
    }


    private final Map<String, AuthorizeParams> store = new HashMap<>();

    /**
     * @inheritDoc
     */
    @NotNull
    public OngoingAuthId store(@NotNull AuthorizeParams p) {

        String key = UUID.randomUUID().toString();
        store.put(key, p);
        return OngoingAuthId.of(key);
    }

    /**
     * @inheritDoc
     */
    @NotNull
    public Optional<AuthorizeParams> find(@NotNull OngoingAuthId key) {
        if (key.getOngoingAuthId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.remove(key.getOngoingAuthId()));
    }

}
