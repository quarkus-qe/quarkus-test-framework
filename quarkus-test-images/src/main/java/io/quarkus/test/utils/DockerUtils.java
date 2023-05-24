package io.quarkus.test.utils;

import static io.quarkus.test.configuration.Configuration.Property.CONTAINER_PREFIX;
import static io.quarkus.test.configuration.Configuration.Property.CONTAINER_REGISTRY_URL;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.google.common.base.Strings;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.services.quarkus.model.LaunchMode;

public final class DockerUtils {

    private static final String DOCKERFILE = "Dockerfile";
    private static final String DOCKERFILE_TEMPLATE = "/Dockerfile.%s";
    private static final Integer DOCKER_PULL_TIMEOUT_SEC = 120;
    private static final String DOCKER = "docker";
    private static final Object LOCK = new Object();
    private static final Random RANDOM = new Random();

    private static DockerClient dockerClientInstance;

    private DockerUtils() {

    }

    public static String getDockerfile(LaunchMode mode) {
        return String.format(DOCKERFILE_TEMPLATE, mode.getName());
    }

    public static String createImageAndPush(ServiceContext service, LaunchMode mode, Path artifact) {
        validateContainerRegistry();

        Path target = getTargetFolder(mode, artifact);

        String dockerfileContent = FileUtils.loadFile(getDockerfile(mode))
                .replaceAll(quote("${ARTIFACT_PARENT}"), target.toString());

        Path dockerfilePath = FileUtils.copyContentTo(dockerfileContent, service.getServiceFolder().resolve(DOCKERFILE));
        buildService(service, dockerfilePath);
        return pushToContainerRegistryUrl(service);
    }

    /**
     * Returns true if docker images contains expectedVersion.
     *
     * @param image docker images
     * @param expectedVersion expected docker image version
     */
    public static boolean isVersion(Image image, String expectedVersion) {
        boolean exist = false;
        String[] tags = Optional.ofNullable(image.getRepoTags()).orElse(new String[] {});
        for (String tag : tags) {
            if (tag.contains(expectedVersion)) {
                exist = true;
                break;
            }
        }

        return exist;
    }

    /**
     * Returns true if docker image is removed.
     *
     * @param name docker image name
     * @param version docker image version
     */
    public static boolean removeImage(String name, String version) {
        boolean removed = false;
        Image image = getImage(name, version);
        if (isVersion(image, version)) {
            stopContainersByImage(image.getId());
            removeImageById(image.getId());
            removed = true;
        }
        return removed;
    }

    /**
     * Remove docker image by image id. Example: consul:1.9.17
     *
     * @param imageId docker image to delete.
     */
    public static void removeImageById(String imageId) {
        dockerClient().removeImageCmd(imageId).withForce(true).exec();
    }

    public static void pullImageById(String imageId) {
        try {
            dockerClient()
                    .pullImageCmd(imageId)
                    .exec(new PullImageResultCallback()).awaitCompletion(DOCKER_PULL_TIMEOUT_SEC, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            fail("Failed to pull image " + imageId + " . Caused by " + e.getMessage());
        }
    }

    /**
     * Stop containers using the image ID.
     *
     * @param imageId docker image ID.
     */
    public static void stopContainersByImage(String imageId) {
        List<Container> containers = dockerClient().listContainersCmd()
                .withAncestorFilter(Collections.singletonList(imageId)).exec();
        for (Container container : containers) {
            dockerClient().stopContainerCmd(container.getId()).exec();
        }
    }

    /**
     * Returns an image based on the provided image name and version. If no image is found then a default empty image.
     * is returned
     *
     * @param name docker image name
     * @param version docker image version
     */
    public static Image getImage(String name, String version) {
        Image result = new Image();
        List<Image> images = dockerClient().listImagesCmd().withImageNameFilter(name).exec();
        for (Image image : images) {
            if (isVersion(image, version)) {
                result = image;
                break;
            }
        }
        return result;
    }

    public static String generateDockerContainerName() {
        String containerName = "" + (RANDOM.nextInt() & Integer.MAX_VALUE);
        String dockerContainerPrefix = new PropertyLookup(CONTAINER_PREFIX.getName()).get();
        if (!Strings.isNullOrEmpty(dockerContainerPrefix)) {
            containerName = dockerContainerPrefix + "-" + containerName;
        }

        return containerName;
    }

    private static DockerClient dockerClient() {
        if (dockerClientInstance == null) {
            synchronized (LOCK) {
                if (dockerClientInstance == null) {
                    DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                            .build();
                    ZerodepDockerHttpClient dockerHttpClient = new ZerodepDockerHttpClient.Builder()
                            .dockerHost(dockerClientConfig.getDockerHost())
                            .sslConfig(dockerClientConfig.getSSLConfig()).build();
                    dockerClientInstance = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
                }
            }
        }

        return dockerClientInstance;
    }

    private static void validateContainerRegistry() {
        if (StringUtils.isEmpty(getRegistryURL())) {
            fail("Container Registry URL is not provided, use -Dts.container.registry-url=XXX");
        }
    }

    private static void buildService(ServiceContext service, Path dockerFile) {
        try {
            new Command(DOCKER, "build", "-f", dockerFile.toString(), "-t", getUniqueName(service), ".").runAndWait();
        } catch (Exception e) {
            fail("Failed to build image " + service.getServiceFolder().toAbsolutePath() + " . Caused by "
                    + e.getMessage());
        }
    }

    private static String pushToContainerRegistryUrl(ServiceContext service) {
        String containerRegistryUrl = getRegistryURL();
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

    private static String getRegistryURL() {
        return new PropertyLookup(CONTAINER_REGISTRY_URL.getName()).get();
    }

    private static String getUniqueName(ServiceContext service) {
        String uniqueName = service.getTestContext().getRequiredTestClass().getName() + "." + service.getName();
        return uniqueName.toLowerCase();
    }

    private static Path getTargetFolder(LaunchMode mode, Path artifact) {
        Path target = artifact.getParent();
        if (mode == LaunchMode.JVM) {
            // remove quarkus-app path
            target = target.getParent();
        }

        return target;
    }
}
