package app.cbo.oidc.java.server.utils;

public class QueryStringBuilder {

    private boolean has = false;
    private final StringBuilder builder=new StringBuilder();

    /**
     * Append an item to the queryString
     *
     * @param fullParam formatted param (with both key & value : 'test=tested'
     * @return this
     */
    public QueryStringBuilder add(String fullParam) {

        if (Utils.isBlank(fullParam))
            return this;
        if (has)
            builder.append("&");
        builder.append(fullParam);
        has = true;

        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
