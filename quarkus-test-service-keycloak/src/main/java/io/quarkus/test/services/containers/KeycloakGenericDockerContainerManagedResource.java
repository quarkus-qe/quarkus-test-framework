package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.DockerUtils;

public class KeycloakGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {

    private static final String PRIVILEGED_MODE = "container.privileged-mode";
    private static final String REUSABLE_MODE = "container.reusable";

    private final KeycloakContainerManagedResourceBuilder model;

    protected KeycloakGenericDockerContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
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

        // Currently, we can't properly set the container's memory limit when running with Podman.
        // More details on this issue can be found here: https://github.com/quarkus-qe/quarkus-test-suite/issues/2106
        container.withEnv("JAVA_OPTS_APPEND", String.format("-XX:MaxRAM=%sm", model.getMemoryLimitMiB()));

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
        return model.getContext().getOwner().getConfiguration().isTrue(REUSABLE_MODE);
    }

    private boolean isPrivileged() {
        return model.getContext().getOwner().getConfiguration().isTrue(PRIVILEGED_MODE);
    }

    static long convertMiBtoBytes(long valueInMiB) {
        final var exponentMiB = 20;
        return (long) (valueInMiB * Math.pow(2, exponentMiB));
    }
}
