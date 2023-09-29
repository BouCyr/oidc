package app.cbo.oidc.java.server.oidc;

import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;

public enum OIDCFlow {
    AUTHORIZATION,
    IMPLICIT,
    HYBRID;


    /**
     * The flow used is determined by the response_type value contained in the Authorization Request. These response_type values select these flows:
     * <p>
     * <p>
     * "response_type" value	|   Flow
     * code 	                |   Authorization Code Flow
     * id_token 	            |   Implicit Flow
     * id_token token 	        |   Implicit Flow
     * code id_token 	        |   Hybrid Flow
     * code token               | 	Hybrid Flow
     * code id_token token      | 	Hybrid Flow
     *
     * @param responseTypes : requested response type in the authorization request (as found in params)
     * @param p             : full params, for error handling
     * @return the corresponding flow
     * @throws AuthErrorInteraction when no flow could be determined from the responseTypes
     */
    public static OIDCFlow fromResponseType(Collection<String> responseTypes, AuthorizeParams p) throws AuthErrorInteraction {

        if (Utils.isEmpty(responseTypes)) {
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
