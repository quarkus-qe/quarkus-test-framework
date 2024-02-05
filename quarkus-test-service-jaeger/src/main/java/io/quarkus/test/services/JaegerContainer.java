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
    String image() default "quay.io/jaegertracing/all-in-one:1.53.0";

    int tracePort() default 16686;

    /**
     * Rest port of a Jaeger collector. Default port 14268 is used by a collector that accepts jaeger.thrift directly from
     * clients.
     */
    int restPort() default 14268;

    /**
     * OTLP port of a Jaeger collector. Default port 4317 is used for the OpenTelemetry Protocol (OTLP) over gRPC.
     */
    int otlpPort() default 4317;

    /**
     * Switches between {@link #restPort()} and {@link #otlpPort()}.
     * If set to true, the OTLP collector is used. If set to false, the Jaeger collector is used.
     */
    boolean useOtlpCollector() default true;

    String expectedLog() default "server started";

    Class<? extends ManagedResourceBuilder> builder() default JaegerContainerManagedResourceBuilder.class;
}
