package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_REST_URL_PROPERTY;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;

public class GrafanaGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {
    public static final int OTEL_GRPC_PORT_NUMBER = 4317;
    public static final int REST_API_PORT_NUMBER = 3100;

    private final GrafanaContainerManagedResourceBuilder model;

    protected GrafanaGenericDockerContainerManagedResource(GrafanaContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(GRAFANA_COLLECTOR_URL_PROPERTY.getName(), getCollectorGrpcUrl());
        model.getContext().put(GRAFANA_REST_URL_PROPERTY.getName(), getRestUrl());
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = super.initContainer();
        container.addExposedPort(model.getOtlpGrpcPort());
        container.addExposedPort(model.getRestPort());
        return container;
    }

    private URILike getCollectorGrpcUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(OTEL_GRPC_PORT_NUMBER));
    }

    private URILike getRestUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(REST_API_PORT_NUMBER));
    }
}
