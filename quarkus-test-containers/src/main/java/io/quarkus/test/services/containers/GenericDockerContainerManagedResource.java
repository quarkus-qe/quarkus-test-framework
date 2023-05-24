package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.DockerUtils;

public class GenericDockerContainerManagedResource extends DockerContainerManagedResource {

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

        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        if (isReusable()) {
            Log.info(model.getContext().getOwner(), "Running container on Reusable mode");
            Log.warn(model.getContext().getOwner(), "Reusable mode expose testcontainers 'withReuse' method that is"
                    + " tagged as UnstableAPI, so is a subject to change and SHOULD NOT be considered a stable API");

            container.withReuse(true);
        }

        container.withExposedPorts(model.getPort());

        return container;
    }

    @Override
    public void stop() {
        if (!isReusable()) {
            super.stop();
        }
    }

    protected boolean isReusable() {
        return model.getContext().getOwner().getConfiguration().isTrue(Configuration.Property.REUSABLE_MODE);
    }

    private boolean isPrivileged() {
        return model.getContext().getOwner().getConfiguration().isTrue(Configuration.Property.PRIVILEGED_MODE);
    }
}
