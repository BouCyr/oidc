package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.oidc.HttpConstants;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParamsHelper {

    public static Map<String, Collection<String>> extractParams(HttpExchange exchange) throws AuthError {
        Map<String, Collection<String>> params;
        if ("GET".equals(exchange.getRequestMethod())) {
            params = QueryStringParser.from(exchange.getRequestURI().getQuery());
        } else if ("POST".equals(exchange.getRequestMethod())) {
            params = readPostBody(exchange);
        } else{
            String msg = "Invalid HTTP method";
            throw new AuthError(AuthError.Code.invalid_request, msg, null, null);
        }
        return params;
    }

    private static Map<String, Collection<String>> readPostBody(HttpExchange exchange) throws AuthError {
        Map<String, Collection<String>> params;
        if (exchange.getRequestHeaders().containsKey("Content-Type")
                && exchange.getRequestHeaders().get("Content-Type").size() == 1
                && HttpConstants.TYPE_FORM.equals(exchange.getRequestHeaders().get("Content-Type").get(0))) {

            // TODO [15/03/2023] autocloseable
            String result = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining("\n"));
            params = QueryStringParser.from(result);

        }else{
            String msg = "POST request on authorization endpoint with wrong contentType";
            throw new AuthError(AuthError.Code.invalid_request, msg, null, null);
        }
        return params;
    }

    /**
     * @param param A collection of String
     * @return the first non empty, non blank value of the list, or empty
     */
    public static Optional<String> singleParam(Collection<String> param){
        if(param == null ||param.isEmpty())
            return Optional.empty();

        //if we have scope=openid email&scope=balbla, we take only the first one
        return param.stream()
                .filter(p -> !p.isBlank())
                .findFirst();
    }

    /**
     * @param spaceSeparatedList A string containing several space separataed values (eg "one two three")
     * @return A list of all non blank non empty values, in order they were found in the source string
     */
    public static List<String> spaceSeparatedList(String spaceSeparatedList){

        return Stream.of(spaceSeparatedList.split(" "))
                .filter(s -> !s.isBlank())
                .toList();
    }
}
