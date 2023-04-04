package app.cbo.oidc.java.server.endpoints;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class HTMLInteraction implements Interaction{


    private final String output;

    public HTMLInteraction(InputStream htmlTemplate, Map<String, String> templatedValues) throws IOException {

        final var outputBuilder = new StringBuilder();
        try(var reader = new BufferedReader(new InputStreamReader(htmlTemplate))){
            String line;
            while((line = reader.readLine())!=null){
                outputBuilder.append(line).append(System.lineSeparator());
            }
        }
        final var templateContent = outputBuilder.toString();

        String output = templateContent;
        for (String key : templatedValues.keySet()) {
            if (output.contains(key)) {
                output = output.replaceAll(key, templatedValues.get(key));
            }
        }
        this.output=output;


    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-type", "text/html");
        exchange.sendResponseHeaders(200, 0);

        try(var is = new ByteArrayInputStream(this.output.getBytes(StandardCharsets.UTF_8));
            var os = exchange.getResponseBody();){
            is.transferTo(os);
            os.flush();
        }
    }

}
