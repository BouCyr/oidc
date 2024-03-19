package app.cbo.oidc.java.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class TestHttpExchange extends HttpExchange {


    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final Map<String, Object> attribute = new HashMap<>();
    private final String method;
    private final URI uri;
    private final InputStream requestBody;
    private final ByteArrayOutputStream responseBody;


    private int statuscode;
    private long responseLength;

    public TestHttpExchange(String method, URI uri, InputStream requestBody) {
        this.method = method;
        this.uri = uri;
        this.requestBody = requestBody;
        this.responseBody = new ByteArrayOutputStream();
    }

    public static TestHttpExchange simpleGet() {
        try {
            return new TestHttpExchange("GET", new URI("http://oidc.cbo.app"), new ByteArrayInputStream(new byte[]{}));
        } catch (URISyntaxException e) {
            //won't happen
            throw new RuntimeException(e);
        }
    }

    public byte[] getResponseBodyBytes() {
        return this.responseBody.toByteArray();
    }


    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
        return uri;
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public InputStream getRequestBody() {
        return requestBody;
    }

    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {

        this.statuscode = rCode;
        this.responseLength = responseLength;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public int getResponseCode() {
        return this.statuscode;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public Object getAttribute(String name) {
        return this.attribute.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attribute.put(name, value);
    }

    @Override
    public void setStreams(InputStream i, OutputStream o) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return new HttpPrincipal("unused", "unused");
    }

    public long getResponseLength() {
        return responseLength;
    }
}
