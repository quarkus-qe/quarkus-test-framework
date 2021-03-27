package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import io.quarkus.test.bootstrap.ServiceContext;

public final class DockerUtils {

    private static final String DOCKER = "docker";

    private DockerUtils() {

    }

    public static void buildService(Path dockerFile, ServiceContext service) {
        try {
            new Command(DOCKER, "build", "-f", dockerFile.toString(), "-t", getUniqueName(service), ".").runAndWait();
        } catch (Exception e) {
            fail("Failed to build image " + service.getServiceFolder().toAbsolutePath().toString() + " . Caused by "
                    + e.getMessage());
        }
    }

    public static String pushToContainerRegistryUrl(ServiceContext service, String containerRegistryUrl) {
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
