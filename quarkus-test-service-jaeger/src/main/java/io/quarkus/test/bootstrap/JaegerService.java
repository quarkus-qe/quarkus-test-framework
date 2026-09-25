package io.quarkus.test.bootstrap;

import static io.quarkus.test.configuration.Configuration.Property.JAEGER_TRACE_URL_PROPERTY;

public class JaegerService extends BaseService<JaegerService> {

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
        return getURI(protocol).toString();
    }

    public String getTraceUrl() {
        return getPropertyFromContext(JAEGER_TRACE_URL_PROPERTY.getName()) + "/api/traces";
    }
}
