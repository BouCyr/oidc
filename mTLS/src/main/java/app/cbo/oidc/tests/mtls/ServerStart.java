package app.cbo.oidc.tests.mtls;

import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class ServerStart {
    final static String SERVER_PWD = "aaaaaa";
    final static String KST_SERVER = "mTLS/keys/server.jks";
    final static String TST_SERVER = "mTLS/keys/servertrust.jks";

    public static HttpsServer server;

    public static void main(String[] args) throws Exception {
//        server = makeServer();
//        server.start();

        var test = HttpServer.create(new InetSocketAddress("0.0.0.0", 7443), 0);
        test.createContext("/test", (x) -> {
            x.sendResponseHeaders(200, 0);
            try (var w = new BufferedWriter(new OutputStreamWriter(x.getResponseBody()))) {
                w.write("OK");
                w.newLine();
            }
        });

        System.out.println("Server running, hit enter to stop.\n");
        System.in.read();


        test.stop(0);
    }

    public static HttpsServer makeServer() throws Exception {
        server = HttpsServer.create(new InetSocketAddress("0.0.0.0", 8443), 0);

        //server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getInstance("TLS"))); // Default config with no auth requirement.
        SSLContext sslCon = createSSLContext();
        MyConfigger authconf = new MyConfigger(sslCon);
        server.setHttpsConfigurator(authconf);

        server.createContext("/auth", new HelloHandler());
        return server;
    }

    private static SSLContext createSSLContext() {
        SSLContext sslContext = null;
        KeyStore ks;
        KeyStore ts;

        try {
            sslContext = SSLContext.getInstance("TLS");

            ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(KST_SERVER), SERVER_PWD.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, SERVER_PWD.toCharArray());

            ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(TST_SERVER), SERVER_PWD.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslContext;
    }
}

class MyConfigger extends HttpsConfigurator {
    public MyConfigger(SSLContext sslContext) {
        super(sslContext);
    }

    @Override
    public void configure(HttpsParameters params) {
        SSLContext sslContext = getSSLContext();
        SSLParameters sslParams = sslContext.getDefaultSSLParameters();
        sslParams.setNeedClientAuth(true);
        params.setNeedClientAuth(true);
        params.setSSLParameters(sslParams);
    }
}

class HelloHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        HttpsExchange ts = (HttpsExchange) t;
        SSLSession sess = ts.getSSLSession();
        //if( sess.getPeerPrincipal() != null) System.out.println(sess.getPeerPrincipal().toString()); // Principal never populated.
        System.out.printf("Responding to host: %s\n", sess.getPeerHost());

        t.getResponseHeaders().set("Content-Type", "text/plain");
        t.sendResponseHeaders(200, 0);
        String response = "Hello!  You seem trustworthy!\n";
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}