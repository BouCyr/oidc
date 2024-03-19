package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.NoResult;
import app.cbo.oidc.java.server.scan.exceptions.TooManyResult;

import java.util.Collection;

class ScanUtils {

    /**
     * Ensure that a collection has only one element, and return it
     * @param items the collection
     * @return the only element
     * @param <U> the type of the elements
     * @throws NoResult if the collection is empty
     * @throws TooManyResult if the collection has more than one element
     */
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
