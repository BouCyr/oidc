package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.scan.Injectable;

public interface PathCustomizer {
    default String customize(String basePath){
        return basePath;
    }

    @Injectable
    class Noop implements PathCustomizer{

    }
}
