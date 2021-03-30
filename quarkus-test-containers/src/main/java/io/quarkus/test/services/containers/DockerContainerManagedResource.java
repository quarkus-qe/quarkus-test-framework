package io.quarkus.test.services.containers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public class DockerContainerManagedResource implements ManagedResource {

    private static final String RESOURCE_PREFIX = "resource::";

    private final ContainerManagedResourceBuilder model;

    private GenericContainer<?> innerContainer;
    private LoggingHandler loggingHandler;

    protected DockerContainerManagedResource(ContainerManagedResourceBuilder model) {
        this.model = model;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        innerContainer = new GenericContainer<>(model.getImage());

        if (StringUtils.isNotBlank(model.getExpectedLog())) {
            innerContainer.waitingFor(new LogMessageWaitStrategy().withRegEx(".*" + model.getExpectedLog() + ".*\\s"));
        }

        if (StringUtils.isNotBlank(model.getCommand())) {
            innerContainer.withCommand(model.getCommand());
        }

        Map<String, String> properties = resolveProperties();
        innerContainer.withEnv(properties);

        innerContainer.withExposedPorts(model.getPort());
        innerContainer.start();

        loggingHandler = new TestContainersLoggingHandler(model.getContext(), innerContainer);
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (isRunning()) {
            innerContainer.stop();
            innerContainer = null;
        }
    }

    @Override
    public int getPort() {
        return innerContainer.getMappedPort(model.getPort());
    }

    @Override
    public String getHost() {
        return "http://" + innerContainer.getHost();
    }

    @Override
    public boolean isRunning() {
        return innerContainer != null && innerContainer.isRunning();
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    private Map<String, String> resolveProperties() {
        Map<String, String> properties = new HashMap<>();
        for (Entry<String, String> entry : model.getContext().getOwner().getProperties().entrySet()) {
            String value = entry.getValue();
            if (isResource(entry.getValue())) {
                value = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                innerContainer.withClasspathResourceMapping(value.substring(1), value, BindMode.READ_ONLY);
            }

            properties.put(entry.getKey(), value);
        }
        return properties;
    }

    private boolean isResource(String key) {
        return key.startsWith(RESOURCE_PREFIX);
    }

}
