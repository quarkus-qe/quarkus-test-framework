package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.BaseService.SERVICE_STARTUP_TIMEOUT;
import static io.quarkus.test.bootstrap.BaseService.SERVICE_STARTUP_TIMEOUT_DEFAULT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.TestContainersLoggingHandler;

public abstract class DockerContainerManagedResource implements ManagedResource {

    private static final String RESOURCE_PREFIX = "resource::";

    private final ServiceContext context;

    private GenericContainer<?> innerContainer;
    private LoggingHandler loggingHandler;

    protected DockerContainerManagedResource(ServiceContext context) {
        this.context = context;
    }

    protected abstract int getTargetPort();

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        innerContainer = initContainer();
        innerContainer.withStartupTimeout(context.getOwner().getConfiguration()
                .getAsDuration(SERVICE_STARTUP_TIMEOUT, SERVICE_STARTUP_TIMEOUT_DEFAULT));
        innerContainer.withEnv(resolveProperties());
        innerContainer.start();

        loggingHandler = new TestContainersLoggingHandler(context.getOwner(), innerContainer);
        loggingHandler.startWatching();
    }

    protected abstract GenericContainer<?> initContainer();

    @Override
    public void stop() {
        if (isRunning()) {
            innerContainer.stop();
            innerContainer = null;
        }
    }

    @Override
    public int getPort(Protocol protocol) {
        return getMappedPort(getTargetPort());
    }

    @Override
    public String getHost(Protocol protocol) {
        return protocol.getValue() + "://" + innerContainer.getHost();
    }

    @Override
    public boolean isRunning() {
        return innerContainer != null && innerContainer.isRunning();
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    protected int getMappedPort(int port) {
        return innerContainer.getMappedPort(port);
    }

    private Map<String, String> resolveProperties() {
        Map<String, String> properties = new HashMap<>();
        for (Entry<String, String> entry : context.getOwner().getProperties().entrySet()) {
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
