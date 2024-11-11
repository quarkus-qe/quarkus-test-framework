package io.quarkus.test.services.containers;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.logging.Log;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public abstract class BaseKafkaContainerManagedResource extends DockerContainerManagedResource {

    private static final String SERVER_PROPERTIES = "kraft/server.properties";
    private static final String EXPECTED_LOG = ".*started .*kafka.server.Kafka.*Server.*";

    protected final KafkaContainerManagedResourceBuilder model;

    private GenericContainer<?> schemaRegistry;
    private TestContainersLoggingHandler schemaRegistryLoggingHandler;
    private Network network;

    protected BaseKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model.getContext());

        this.model = model;
    }

    protected abstract GenericContainer<?> initKafkaContainer();

    protected abstract GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka);

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
        return StringUtils.defaultIfBlank(model.getVersion(), model.getVendor().getDefaultVersion());
    }

    protected String getKafkaImageName() {
        return StringUtils.defaultIfBlank(model.getImage(), model.getVendor().getImage());
    }

    protected String getKafkaRegistryImage() {
        return model.getRegistryImageVersion();
    }

    protected int getKafkaRegistryPort() {
        return model.getVendor().getRegistry().getPort();
    }

    protected String getResourceTargetName(String resource) {
        return resource;
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> kafkaContainer = initKafkaContainer();

        kafkaContainer.waitingFor(Wait.forLogMessage(EXPECTED_LOG, 1));

        String kafkaConfigPath = model.getKafkaConfigPath();
        if (StringUtils.isNotEmpty(getServerProperties())) {
            Log.info("Copying file %s to %s ", getServerProperties(), kafkaConfigPath + SERVER_PROPERTIES);
            kafkaContainer.withCopyFileToContainer(MountableFile.forClasspathResource(getServerProperties()),
                    kafkaConfigPath + SERVER_PROPERTIES);
        }

        for (String resource : getKafkaConfigResources()) {
            if (resource.contains(File.separator)) {
                // file in the target directory
                String fileName = resource.substring(resource.lastIndexOf(File.separator) + 1);
                kafkaContainer.withCopyFileToContainer(MountableFile.forHostPath(resource), kafkaConfigPath + fileName);
            } else {
                // resource
                kafkaContainer.withCopyFileToContainer(MountableFile.forClasspathResource(resource),
                        kafkaConfigPath + getResourceTargetName(resource));
            }
        }

        if (model.isWithRegistry()) {
            schemaRegistry = initRegistryContainer(kafkaContainer);
            schemaRegistryLoggingHandler = new TestContainersLoggingHandler(model.getContext().getOwner(), schemaRegistry);

            // Setup common network for kafka and the registry
            network = Network.newNetwork();
            kafkaContainer.withNetwork(network);
            schemaRegistry.withNetwork(network);
        }

        return kafkaContainer;
    }

    @Override
    protected int getTargetPort() {
        return model.getVendor().getPort();
    }

    protected String[] getKafkaConfigResources() {
        return model.getKafkaConfigResources();
    }

    protected String getServerProperties() {
        return model.getServerProperties();
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
        String path = StringUtils.defaultIfBlank(model.getRegistryPath(), model.getVendor().getRegistry().getPath());
        String containerIp = schemaRegistry.getHost();
        return String.format("http://%s:%s%s", containerIp, schemaRegistry.getMappedPort(getKafkaRegistryPort()), path);
    }

}
