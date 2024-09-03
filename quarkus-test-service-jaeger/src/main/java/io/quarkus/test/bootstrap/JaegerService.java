package io.quarkus.test.bootstrap;

public class JaegerService extends BaseService<JaegerService> {

    public static final String JAEGER_TRACE_URL_PROPERTY = "ts.jaeger.trace.url";
    public static final String JAEGER_API_PATH = "/api/traces";

    /**
     * Deprecated, call {@link #getCollectorUrl()} directly.
     */
    @Deprecated
    public String getRestUrl() {
        return getCollectorUrl();
    }

    public String getCollectorUrl() {
        return getCollectorUrl(Protocol.HTTP);
    }

    public String getCollectorUrl(Protocol protocol) {
        return getURI(protocol).withPath(JAEGER_API_PATH).toString();
    }

    public String getTraceUrl() {
        return getPropertyFromContext(JAEGER_TRACE_URL_PROPERTY) + JAEGER_API_PATH;
    }
}
