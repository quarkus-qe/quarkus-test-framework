package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_LOKI_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_PROMETHEUS_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_TEMPO_URL_PROPERTY;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;

public class GrafanaGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {
    public static final int OTEL_GRPC_PORT_NUMBER = 4317;
    public static final int LOKI_API_PORT_NUMBER = 3100;
    public static final int TEMPO_API_PORT_NUMBER = 3200;
    public static final int PROMETHEUS_API_PORT_NUMBER = 9090;

    private final GrafanaContainerManagedResourceBuilder model;

    protected GrafanaGenericDockerContainerManagedResource(GrafanaContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(GRAFANA_COLLECTOR_URL_PROPERTY.getName(), getCollectorGrpcUrl());
        model.getContext().put(GRAFANA_LOKI_URL_PROPERTY.getName(), getLokiUrl());
        model.getContext().put(GRAFANA_TEMPO_URL_PROPERTY.getName(), getTempoUrl());
        model.getContext().put(GRAFANA_PROMETHEUS_URL_PROPERTY.getName(), getPrometheusUrl());
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = super.initContainer();
        container.addExposedPort(model.getOtlpGrpcPort());
        container.addExposedPort(model.getLokiPort());
        container.addExposedPort(model.getTempoPort());
        container.addExposedPort(model.getPrometheusPort());
        return container;
    }

    private URILike getCollectorGrpcUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(OTEL_GRPC_PORT_NUMBER));
    }

    private URILike getLokiUrl() {
        return getURI(Protocol.HTTP).withPort(getMappedPort(model.getLokiPort()));
    }

    private URILike getTempoUrl() {
        return getURI(Protocol.HTTP).withPort(getMappedPort(model.getTempoPort()));
    }

    private URILike getPrometheusUrl() {
        return getURI(Protocol.HTTP).withPort(getMappedPort(model.getPrometheusPort()));
    }
}
