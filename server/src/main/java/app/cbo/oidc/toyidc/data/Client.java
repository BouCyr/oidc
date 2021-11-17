package app.cbo.oidc.toyidc.data;

public class Client {
    public String clientId;

    /**
     * Base redirect uri (used
     */
    public String redirectUri;

    /**
     * check if the redirect_uri passed in a request is valid for this client
     * @param uri
     * @return
     */
    public boolean validateRedirectUri(String uri){
        //TODO [17/11/2021]
        //this.redirect_uri, with additionnal query params added accepte
        return true;
    }
}
