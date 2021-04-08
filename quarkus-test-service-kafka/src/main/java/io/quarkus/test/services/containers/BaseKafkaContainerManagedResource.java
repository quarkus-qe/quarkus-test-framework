package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public abstract class BaseKafkaContainerManagedResource extends DockerContainerManagedResource {

    private final KafkaContainerManagedResourceBuilder model;

    private GenericContainer<?> schemaRegistry;
    private TestContainersLoggingHandler schemaRegistryLoggingHandler;
    private Network network;

    protected BaseKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model.getContext());

        this.model = model;
    }

    protected abstract String getDefaultKafkaVersion();

    protected abstract int getRegistryTargetPort();

    protected abstract GenericContainer<?> initKafkaContainer();

    protected abstract GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka);

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> kafkaContainer = initKafkaContainer();

        if (model.isWithRegistry()) {
            schemaRegistry = initRegistryContainer(kafkaContainer);
            schemaRegistryLoggingHandler = new TestContainersLoggingHandler(model.getContext(), schemaRegistry);

            // Setup common network for kafka and the registry
            network = Network.newNetwork();
            kafkaContainer.withNetwork(network);
            schemaRegistry.withNetwork(network);
        }

        return kafkaContainer;
    }

    @Override
    public void start() {
        super.start();

        startRegistryIfEnabled();
    }

    @Override
    public void stop() {
        super.stop();

        stopRegistryIfEnabled();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() && (!model.isWithRegistry() || isRegistryRunning());
    }

    protected String getKafkaVersion() {
        return StringUtils.defaultIfBlank(model.getVersion(), getDefaultKafkaVersion());
    }

    protected String getRegistryPath() {
        return StringUtils.EMPTY;
    }

    private void startRegistryIfEnabled() {
        if (model.isWithRegistry()) {
            schemaRegistryLoggingHandler.startWatching();

            if (!isRegistryRunning()) {
                schemaRegistry.start();
            }

            model.getContext().put(KafkaService.KAFKA_REGISTRY_URL_PROPERTY, getSchemaRegistryUrl());
        }
    }

    private void stopRegistryIfEnabled() {
        if (model.isWithRegistry() && isRegistryRunning()) {
            schemaRegistryLoggingHandler.stopWatching();
            schemaRegistry.stop();
        }

        if (network != null) {
            network.close();
        }
    }

    private boolean isRegistryRunning() {
        return schemaRegistry != null && schemaRegistry.isRunning();
    }

    private String getSchemaRegistryUrl() {
        return "http://" + schemaRegistry.getContainerIpAddress() + ":" + schemaRegistry.getMappedPort(getRegistryTargetPort())
                + getRegistryPath();
    }

}
