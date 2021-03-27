package io.quarkus.test.services.containers;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public class DockerContainerManagedResource implements ManagedResource {

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

        innerContainer.withEnv(model.getContext().getOwner().getProperties());

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
        return innerContainer.getHost();
    }

    @Override
    public boolean isRunning() {
        return innerContainer != null && innerContainer.isRunning();
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

}
