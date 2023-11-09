package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.NoResult;
import app.cbo.oidc.java.server.scan.exceptions.TooManyResult;

import java.util.Collection;

class ScanUtils {
    public static <U> U oneAndOnlyOne(Collection<U> constructors) throws NoResult, TooManyResult {
        if (constructors == null || constructors.size() == 0) {
            throw new NoResult();
        }
        if (constructors.size() > 1) {
            throw new TooManyResult(constructors.size());
        }
        return constructors.iterator().next();
    }
}
