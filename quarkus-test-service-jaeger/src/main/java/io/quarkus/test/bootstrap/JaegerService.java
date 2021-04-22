package io.quarkus.test.bootstrap;

public class JaegerService extends BaseService<JaegerService> {

    public static final String JAEGER_TRACE_URL_PROPERTY = "ts.jaeger.trace.url";

    public String getJaegerRestUrl() {
        return getHost() + ":" + getPort();
    }

    public String getJaegerTraceUrl() {
        return getPropertyFromContext(JAEGER_TRACE_URL_PROPERTY);
    }
}
