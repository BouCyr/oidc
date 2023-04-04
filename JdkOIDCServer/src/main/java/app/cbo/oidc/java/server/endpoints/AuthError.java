package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;
import app.cbo.oidc.java.server.oidc.HttpConstants;
import app.cbo.oidc.java.server.utils.ExceptionHandling;
import app.cbo.oidc.java.server.utils.ExchangeResponseUtils;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.Utils;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public  class AuthError extends Exception implements Interaction {

    private final static Logger LOGGER = Logger.getLogger(AuthError.class.getCanonicalName());



    public enum Code {
        invalid_request,
        unauthorized_client,
        access_denied,
        unsupported_response_type,
        invalid_scope,
        server_error,
        temporarily_unavailable
    }


    private final Code error;
    private final String errorDescription;
    private final String redirectUri;
    private final String state;

    public AuthError(Code error, String errorDescription){
        this(error, errorDescription, null, null);
    }

    public AuthError(Code error, String errorDescription, AuthorizeEndpointParams params){

        this(error, errorDescription, params.redirectUri().orElse(null), params.state().orElse(null));
    }

    public AuthError(Code error, String errorDescription, String redirectUri, String state) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {


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

            try {
                exchange.getResponseHeaders().add("Location", redirectTo);
            }catch(RuntimeException e){
                throw e;
            }
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

            ExchangeResponseUtils.build(exchange, status, HttpConstants.TYPE_TEXT_PLAIN, content);
        }

    }
}
