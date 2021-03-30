package io.quarkus.test.utils;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.services.quarkus.model.LaunchMode;

public final class DockerUtils {

    public static final String CONTAINER_REGISTY_URL_PROPERTY = "ts.container.registry-url";

    private static final PropertyLookup CONTAINER_REGISTY_URL = new PropertyLookup(CONTAINER_REGISTY_URL_PROPERTY);

    private static final String DOCKERFILE = "Dockerfile";
    private static final String DOCKERFILE_TEMPLATE = "/Dockerfile.%s";

    private static final String DOCKER = "docker";

    private DockerUtils() {

    }

    public static String getDockerfile(LaunchMode mode) {
        return String.format(DOCKERFILE_TEMPLATE, mode.getName());
    }

    public static String createImageAndPush(ServiceContext service, LaunchMode mode, Path artifact) {
        validateContainerRegistry(service);

        String dockerfileContent = FileUtils.loadFile(getDockerfile(mode))
                .replaceAll(quote("${ARTIFACT_PARENT}"), artifact.getParent().toString());

        Path dockerfilePath = FileUtils.copyContentTo(dockerfileContent, service.getServiceFolder().resolve(DOCKERFILE));
        buildService(service, dockerfilePath);
        return pushToContainerRegistryUrl(service);
    }

    private static void validateContainerRegistry(ServiceContext service) {
        if (StringUtils.isEmpty(CONTAINER_REGISTY_URL.get(service))) {
            fail("Container Registry URL is not provided, use -Dts.container.registry-url=XXX");
        }
    }

    private static void buildService(ServiceContext service, Path dockerFile) {
        try {
            new Command(DOCKER, "build", "-f", dockerFile.toString(), "-t", getUniqueName(service), ".").runAndWait();
        } catch (Exception e) {
            fail("Failed to build image " + service.getServiceFolder().toAbsolutePath().toString() + " . Caused by "
                    + e.getMessage());
        }
    }

    private static String pushToContainerRegistryUrl(ServiceContext service) {
        String containerRegistryUrl = CONTAINER_REGISTY_URL.get(service);
        try {
            String targetImage = containerRegistryUrl + "/" + getUniqueName(service);
            new Command(DOCKER, "tag", getUniqueName(service), targetImage).runAndWait();
            new Command(DOCKER, "push", targetImage).runAndWait();
            return targetImage;
        } catch (Exception e) {
            fail("Failed to push image " + service.getOwner().getName() + " into " + containerRegistryUrl + ". Caused by "
                    + e.getMessage());
        }

        return null;
    }

    private static String getUniqueName(ServiceContext service) {
        String uniqueName = service.getTestContext().getRequiredTestClass().getName() + "." + service.getName();
        return uniqueName.toLowerCase();
    }
}
