package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.BaseService.SERVICE_STARTUP_TIMEOUT;
import static io.quarkus.test.bootstrap.BaseService.SERVICE_STARTUP_TIMEOUT_DEFAULT;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX_MATCHER;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_SPLIT_CHAR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.TestContainersLoggingHandler;
import io.quarkus.test.utils.DockerUtils;

public abstract class DockerContainerManagedResource implements ManagedResource {

    private static final String DELETE_IMAGE_ON_STOP_PROPERTY = "container.delete.image.on.stop";
    private static final String TARGET = "target";

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

        loggingHandler = new TestContainersLoggingHandler(context.getOwner(), innerContainer);
        loggingHandler.startWatching();

        doStart();
    }

    protected abstract GenericContainer<?> initContainer();

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        String image = innerContainer.getImage().get();

        if (isRunning()) {
            innerContainer.stop();
            innerContainer = null;
        }

        if (context.getOwner().getConfiguration().isTrue(DELETE_IMAGE_ON_STOP_PROPERTY)) {
            DockerUtils.removeImageById(image);
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

    private void doStart() {
        try {
            innerContainer.start();
        } catch (Exception ex) {
            stop();

            throw ex;
        }

    }

    private Map<String, String> resolveProperties() {
        Map<String, String> properties = new HashMap<>();
        for (Entry<String, String> entry : context.getOwner().getProperties().entrySet()) {
            String value = entry.getValue();
            if (isResource(entry.getValue())) {
                value = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                addFileToContainer(value);
            } else if (isResourceWithDestinationPath(entry.getValue())) {
                value = entry.getValue().replace(RESOURCE_WITH_DESTINATION_PREFIX, StringUtils.EMPTY);
                if (!value.matches(RESOURCE_WITH_DESTINATION_PREFIX_MATCHER)) {
                    String errorMsg = String.format("Unexpected %s format. Expected destinationPath|fileName but found %s",
                            RESOURCE_WITH_DESTINATION_PREFIX, value);
                    throw new RuntimeException(errorMsg);
                }

                String destinationPath = value.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[0];
                String fileName = value.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[1];
                addFileToContainer(destinationPath, fileName);
            } else if (isSecret(entry.getValue())) {
                value = entry.getValue().replace(SECRET_PREFIX, StringUtils.EMPTY);
                addFileToContainer(value);
            }

            properties.put(entry.getKey(), value);
        }
        return properties;
    }

    private void addFileToContainer(String filePath) {
        if (Files.exists(Path.of(TARGET, filePath))) {
            // Mount file if it's a file
            innerContainer.withCopyFileToContainer(MountableFile.forHostPath(Path.of(TARGET, filePath)), filePath);
        } else {
            // then file is in the classpath
            innerContainer.withClasspathResourceMapping(filePath, filePath, BindMode.READ_ONLY);
        }
    }

    private void addFileToContainer(String destinationPath, String hostFilePath) {
        String containerFullPath = destinationPath + hostFilePath;
        innerContainer.withClasspathResourceMapping(hostFilePath, containerFullPath, BindMode.READ_ONLY);
    }

    private boolean isResourceWithDestinationPath(String key) {
        return key.startsWith(RESOURCE_WITH_DESTINATION_PREFIX);
    }

    private boolean isResource(String key) {
        return key.startsWith(RESOURCE_PREFIX);
    }

    private boolean isSecret(String key) {
        return key.startsWith(SECRET_PREFIX);
    }

}
