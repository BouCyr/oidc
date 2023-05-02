package app.cbo.oidc.java.server.credentials;

import app.cbo.oidc.java.server.utils.Utils;

import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Logger;

public record AuthenticationLevel(String name, int level) {

    private final static Logger LOGGER = Logger.getLogger(AuthenticationLevel.class.getCanonicalName());


    public AuthenticationLevel(EnumSet<AuthenticationMode> authentications) {
        this("level" + authentications.size(), authentications.size());
    }

    /**
     * Parse an ACR level from the string put in the idtoken
     *
     * @param acr as found in the idtoekn/authorization request and so on
     * @return an optional containing the level if somethign could be recognized from the input. Optional.empty() if not
     */
    public static Optional<AuthenticationLevel> fromAcr(String acr) {

        if (Utils.isBlank(acr)) {
            LOGGER.info("Invalid acr");
            return Optional.empty();
        }
        if (!acr.startsWith("level")) {
            LOGGER.info(acr + "is not an acr recognized by the system.");
        }

        try {
            int level = Integer.parseInt(acr.substring("level".length()));
            return Optional.of(new AuthenticationLevel(acr, level));
        } catch (Exception e) {
            LOGGER.info(acr + "is not an acr recognized by the system.");
            return Optional.empty();
        }
    }
}
