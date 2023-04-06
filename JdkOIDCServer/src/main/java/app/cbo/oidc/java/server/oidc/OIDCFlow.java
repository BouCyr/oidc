package app.cbo.oidc.java.server.oidc;

import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;

public enum OIDCFlow {
    AUTHORIZATION,
    IMPLICIT,
    HYBRID;

    public static OIDCFlow fromResponseType(Collection<String> responseTypes, AuthorizeParams p) throws AuthErrorInteraction {

        if(Utils.isEmpty(responseTypes)){
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.unsupported_response_type, "response_type is REQUIRED", p);
        }

        boolean hasCode = responseTypes.contains("code");
        boolean hasIdToken = responseTypes.contains("id_token");
        boolean hasToken = responseTypes.contains("token");

        if(responseTypes.size()==1){
            if(hasCode)
                return AUTHORIZATION;
            if(hasIdToken)
                return IMPLICIT;
        } else if(responseTypes.size()==2){
            if(hasIdToken && hasToken)
                return IMPLICIT;
            if(hasCode && (hasIdToken || hasToken))
                return HYBRID;

        } else if(responseTypes.size() == 3){
            if(hasCode && hasIdToken && hasToken)
                return HYBRID;
        }
        throw new AuthErrorInteraction(AuthErrorInteraction.Code.unsupported_response_type, "Invalid response_type", p);

    }
}
