package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public  class AuthErrorInteraction extends Exception implements Interaction {

    private final static Logger LOGGER = Logger.getLogger(AuthErrorInteraction.class.getCanonicalName());


    public enum Code {
        invalid_request,
        unauthorized_client,
        invalid_client,
        access_denied,
        unsupported_response_type,
        invalid_scope,
        server_error,
        temporarily_unavailable,
        invalid_grant
    }


    private final Code error;
    private final String errorDescription;
    private final String redirectUri;
    private final String state;

    public AuthErrorInteraction(Code error, String errorDescription){
        this(error, errorDescription, null, null);
    }

    public AuthErrorInteraction(Code error, String errorDescription, AuthorizeParams params){

        this(error, errorDescription, params.redirectUri().orElse(null), params.state().orElse(null));
    }

    public AuthErrorInteraction(Code error, String errorDescription, String redirectUri, String state) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {


        LOGGER.info(String.format("Handling error with code %s and description %s ", error, Utils.isBlank(errorDescription)?"NONE":errorDescription));

        LOGGER.info(ExceptionHandling.getStackTrace(this));




        if(!Utils.isBlank(this.state)){
            LOGGER.info(String.format("We have a state %s and a redirect_uri %s",
                    Utils.isBlank(this.state)?"NO":state,
                    Utils.isBlank(this.redirectUri)?"NO":redirectUri));
        }

        if(redirectUri != null){

            String redirectParams =
                "error="+error.name()
                +"&error_description="+ URLEncoder.encode(errorDescription, Charset.defaultCharset());

            if(state != null){
                redirectParams+=("&state="+state);
            }

            boolean ruHasParams = redirectUri.contains("?");//TODO [17/03/2023] Seems  a little light

            String redirectTo = redirectUri
                    +(ruHasParams?"&":"?")
                    +redirectParams;

            exchange.getResponseHeaders().add("Location", redirectTo);
            ExchangeResponseUtils.build(exchange, HttpCode.FOUND, null, null);
        }else{
            LOGGER.info("Returning httpStatus 500 since we do not know where to send the error.");
            HttpCode status= HttpCode.BAD_REQUEST;
            if(error == Code.server_error)
                status= HttpCode.SERVER_ERROR;
            if(error == Code.temporarily_unavailable)
                status = HttpCode.UNAVAILABLE;


            String content = error.name()
                    +System.lineSeparator()
                    +errorDescription;

            ExchangeResponseUtils.build(exchange, status, MimeType.TEXT_PLAIN.mimeType(), content);
        }

    }
}
