package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.NoResult;
import app.cbo.oidc.java.server.scan.exceptions.TooManyResult;

import java.util.Collection;

class ScanUtils {
    public static <U> U oneAndOnlyOne(Collection<U> items) throws NoResult, TooManyResult {
        if (items == null || items.size() == 0) {
            throw new NoResult();
        }
        if (items.size() > 1) {
            throw new TooManyResult(items.size());
        }
        return items.iterator().next();
    }
}
