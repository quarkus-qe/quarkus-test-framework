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

    /**
     * @deprecated Use {@link #getTraceV3Url()} for Jaeger 2.x v3 API.
     */
    @Deprecated
    public String getTraceUrl() {
        return getPropertyFromContext(JAEGER_TRACE_URL_PROPERTY.getName()) + "/api/traces";
    }

    public String getTraceV3Url() {
        return getPropertyFromContext(JAEGER_TRACE_URL_PROPERTY.getName()) + "/api/v3/traces";
    }
}
