package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

public class GenericDockerContainerManagedResource extends DockerContainerManagedResource {

    private static final String PRIVILEGED_MODE = "container.privileged-mode";
    private final ContainerManagedResourceBuilder model;

    protected GenericDockerContainerManagedResource(ContainerManagedResourceBuilder model) {
        super(model.getContext());
        this.model = model;
    }

    @Override
    protected int getTargetPort() {
        return model.getPort();
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = new GenericContainer<>(model.getImage());

        if (StringUtils.isNotBlank(model.getExpectedLog())) {
            container.waitingFor(new LogMessageWaitStrategy().withRegEx(".*" + model.getExpectedLog() + ".*\\s"));
        }

        if (model.getCommand() != null && model.getCommand().length > 0) {
            container.withCommand(model.getCommand());
        }

        container.setPrivilegedMode(isPrivileged());

        container.withExposedPorts(model.getPort());

        return container;
    }

    private boolean isPrivileged() {
        return model.getContext().getOwner().getConfiguration().isTrue(PRIVILEGED_MODE);
    }
}
