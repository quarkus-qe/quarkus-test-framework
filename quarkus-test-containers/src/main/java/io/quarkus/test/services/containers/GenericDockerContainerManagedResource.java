package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.logging.Log;

public class GenericDockerContainerManagedResource extends DockerContainerManagedResource {

    private static final String PRIVILEGED_MODE = "container.privileged-mode";
    private final ContainerManagedResourceBuilder model;

    protected GenericDockerContainerManagedResource(ContainerManagedResourceBuilder model) {
        super(model.getContext());
        this.model = model;
    }

    @Override
    public String getDisplayName() {
        return model.getImage();
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

        if (isPrivileged()) {
            Log.info(model.getContext().getOwner(), "Running container on Privileged mode");
            container.setPrivilegedMode(true);
        }

        container.withExposedPorts(model.getPort());

        container.withReuse(model.isReusable());
        return container;
    }

    private boolean isPrivileged() {
        return model.getContext().getOwner().getConfiguration().isTrue(PRIVILEGED_MODE);
    }
}
