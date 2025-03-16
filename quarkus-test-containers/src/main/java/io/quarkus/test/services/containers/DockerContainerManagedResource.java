package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.BaseService.SERVICE_STARTUP_TIMEOUT_DEFAULT;
import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX_MATCHER;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_SPLIT_CHAR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SLASH;

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
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.TestContainersLoggingHandler;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.FileUtils;

public abstract class DockerContainerManagedResource implements ManagedResource {

    public static final String DOCKER_INNER_CONTAINER = DockerContainerManagedResource.class.getName() + "_inner";
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

        if (isDockerImageDeletedOnStop()) {
            String image = innerContainer.getImage().get();
            DockerUtils.pullImageById(image);
        }

        Configuration configuration = context.getOwner().getConfiguration();
        innerContainer.withStartupTimeout(configuration
                .getAsDuration(Configuration.Property.SERVICE_STARTUP_TIMEOUT, SERVICE_STARTUP_TIMEOUT_DEFAULT));
        innerContainer.withEnv(resolveProperties());
        innerContainer.withStartupAttempts(configuration.getAsInteger(Configuration.Property.CONTAINER_STARTUP_ATTEMPTS, 1));

        loggingHandler = new TestContainersLoggingHandler(context.getOwner(), innerContainer);
        loggingHandler.startWatching();

        doStart();

        context.put(DOCKER_INNER_CONTAINER, innerContainer);
    }

    private boolean isDockerImageDeletedOnStop() {
        return context.getOwner().getConfiguration().isTrue(Configuration.Property.DELETE_IMAGE_ON_STOP_PROPERTY);
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

        if (isDockerImageDeletedOnStop()) {
            DockerUtils.removeImageById(image);
        }
    }

    @Override
    public URILike getURI(Protocol protocol) {
        return createURI(protocol.getValue(), innerContainer.getHost(), getMappedPort(getTargetPort()));
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
            loggingHandler.logs().forEach(Log::info);
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
            } else if (isSecretWithDestinationPath(entry.getValue())) {
                value = entry.getValue().replace(SECRET_WITH_DESTINATION_PREFIX, StringUtils.EMPTY);
                String destinationPath = value.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[0];
                String fileName = value.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[1];
                addFileToContainer(destinationPath, fileName);
                value = value.replace(DESTINATION_TO_FILENAME_SEPARATOR, "");
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
                value = value.replace(DESTINATION_TO_FILENAME_SEPARATOR, "");
            } else if (isSecret(entry.getValue())) {
                value = entry.getValue().replace(SECRET_PREFIX, StringUtils.EMPTY);
                addFileToContainer(value);
            }

            properties.put(entry.getKey(), value);
        }
        return properties;
    }

    private void addFileToContainer(String path) {
        Path filePath = Path.of(TARGET, path);
        if (Files.exists(filePath)) {
            // Mount file if it's a file
            innerContainer.withCopyFileToContainer(MountableFile.forHostPath(filePath), path);
        } else {
            // then file is in the classpath
            innerContainer.withClasspathResourceMapping(path, path, BindMode.READ_ONLY);
        }
    }

    private void addFileToContainer(String destinationPath, String hostFilePath) {
        var filePath = FileUtils.findTargetFile(Path.of("target"), hostFilePath);
        String containerFullPath = destinationPath + SLASH + hostFilePath;
        if (filePath.isEmpty()) {
            innerContainer.withClasspathResourceMapping(hostFilePath, containerFullPath, BindMode.READ_ONLY);
        } else {
            innerContainer.withCopyFileToContainer(MountableFile.forHostPath(filePath.get()), containerFullPath);
        }
    }

    private static boolean isSecretWithDestinationPath(String key) {
        return key.startsWith(SECRET_WITH_DESTINATION_PREFIX);
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
