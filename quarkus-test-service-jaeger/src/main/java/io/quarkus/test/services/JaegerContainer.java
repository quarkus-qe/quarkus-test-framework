package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.JaegerContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JaegerContainer {
    String image() default "quay.io/jaegertracing/jaeger:2.22.0";

    int tracePort() default 16686;

    /**
     * @deprecated Jaeger 2.x only supports OTLP. Use {@link #otlpPort()} instead.
     */
    @Deprecated
    int restPort() default 14268;

    /**
     * OTLP HTTP port of the Jaeger collector. Default port 4318 is used for the OpenTelemetry Protocol (OTLP) over
     * http/protobuf.
     */
    int otlpPort() default 4318;

    /**
     * @deprecated Jaeger 2.x always has OTLP enabled. This attribute is ignored.
     */
    @Deprecated
    boolean useOtlpCollector() default true;

    String expectedLog() default "Everything is ready. Begin running and processing data.";

    /**
     * @return whether communication between Quarkus OTel exporter and Jaeger OTLP collector should be secured
     */
    boolean tls() default false;

    Class<? extends ManagedResourceBuilder> builder() default JaegerContainerManagedResourceBuilder.class;
}
