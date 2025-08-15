package io.quarkus.test.services;

import static io.quarkus.test.services.containers.GrafanaGenericDockerContainerManagedResource.LOKI_API_PORT_NUMBER;
import static io.quarkus.test.services.containers.GrafanaGenericDockerContainerManagedResource.OTEL_GRPC_PORT_NUMBER;
import static io.quarkus.test.services.containers.GrafanaGenericDockerContainerManagedResource.PROMETHEUS_API_PORT_NUMBER;
import static io.quarkus.test.services.containers.GrafanaGenericDockerContainerManagedResource.TEMPO_API_PORT_NUMBER;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.GrafanaContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrafanaContainer {
    String image() default "grafana/otel-lgtm:0.8.2";

    /**
     * Port where web UI is available.
     */
    int webUIPort() default 3000;

    /**
     * Port where Loki REST API is available.
     */
    int lokiPort() default LOKI_API_PORT_NUMBER;

    /**
     * Port where Tempo REST API is available.
     */
    int tempoPort() default TEMPO_API_PORT_NUMBER;

    /**
     * Port where Prometheus REST API is available.
     */
    int prometheusPort() default PROMETHEUS_API_PORT_NUMBER;

    /**
     * OTLP port of collector. Default port 4317 is used for the OpenTelemetry Protocol (OTLP) over gRPC.
     */
    int otlpGrpcPort() default OTEL_GRPC_PORT_NUMBER;

    /**
     * Expected log line, which indicates that service is fully booted.
     */
    String expectedLog() default "Grafana LGTM stack are up and running";

    Class<? extends ManagedResourceBuilder> builder() default GrafanaContainerManagedResourceBuilder.class;
}
