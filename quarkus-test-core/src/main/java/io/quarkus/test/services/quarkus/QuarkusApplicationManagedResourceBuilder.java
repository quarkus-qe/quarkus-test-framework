package io.quarkus.test.services.quarkus;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;

public abstract class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    public static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    public static final String QUARKUS_GRPC_SERVER_PORT_PROPERTY = "quarkus.grpc.server.port";
    public static final String QUARKUS_HTTP_SSL_PORT_PROPERTY = "quarkus.http.ssl-port";
    public static final int HTTP_PORT_DEFAULT = 8080;

    protected static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources");

    private static final String BUILD_TIME_PROPERTIES = "/build-time-list";
    private static final Path TEST_RESOURCES_FOLDER = Paths.get("src", "test", "resources");
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final Set<String> BUILD_PROPERTIES = FileUtils.loadFile(BUILD_TIME_PROPERTIES).lines().collect(toSet());

    private Class<?>[] appClasses;
    private boolean selectedAppClasses = true;
    private ServiceContext context;
    private boolean sslEnabled = false;
    private boolean grpcEnabled = false;
    private Map<String, String> propertiesSnapshot;

    protected abstract void build();

    protected ServiceContext getContext() {
        return context;
    }

    protected void setContext(ServiceContext context) {
        this.context = context;
    }

    protected boolean isSslEnabled() {
        return sslEnabled;
    }

    protected void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    protected boolean isGrpcEnabled() {
        return grpcEnabled;
    }

    protected void setGrpcEnabled(boolean grpcEnabled) {
        this.grpcEnabled = grpcEnabled;
    }

    protected Class<?>[] getAppClasses() {
        return appClasses;
    }

    protected boolean isSelectedAppClasses() {
        return selectedAppClasses;
    }

    @Override
    public String getComputedProperty(String name) {
        Path applicationProperties = getComputedApplicationProperties();
        if (!Files.exists(applicationProperties)) {
            // computed properties have not been propagated yet, we use the one from src/main/resources
            applicationProperties = RESOURCES_FOLDER.resolve(APPLICATION_PROPERTIES);
        }

        if (!Files.exists(applicationProperties)) {
            return null;
        }

        Map<String, String> computedProperties = PropertiesUtils.toMap(applicationProperties);
        return Optional.ofNullable(computedProperties.get(name))
                .orElseGet(() -> computedProperties.get(propertyWithProfile(name)));
    }

    public boolean containsBuildProperties() {
        Map<String, String> differenceProperties = MapUtils.difference(context.getOwner().getProperties(), propertiesSnapshot);
        Set<String> properties = differenceProperties.keySet();
        if (properties.isEmpty()) {
            return false;
        }

        return properties.stream().anyMatch(this::isBuildProperty);
    }

    public Map<String, String> createSnapshotOfBuildProperties() {
        propertiesSnapshot = new HashMap<>(context.getOwner().getProperties());
        return new HashMap<>(context.getOwner().getProperties());
    }

    public Map<String, String> getBuildProperties() {
        return getContext().getOwner().getProperties().entrySet().stream()
                .filter(e -> isBuildProperty(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void initAppClasses(Class<?>[] classes) {
        appClasses = classes;
        if (appClasses == null || appClasses.length == 0) {
            appClasses = ClassPathUtils.findAllClassesFromSource();
            selectedAppClasses = false;
        }
    }

    protected void configureLogging() {
        context.getOwner().withProperty("quarkus.log.console.format", "%d{HH:mm:ss,SSS} %s%e%n");
    }

    protected void copyResourcesToAppFolder() {
        copyResourcesInFolderToAppFolder(RESOURCES_FOLDER);
        copyResourcesInFolderToAppFolder(TEST_RESOURCES_FOLDER);
        createComputedApplicationProperties();
    }

    protected Path getApplicationFolder() {
        return context.getServiceFolder();
    }

    protected Path getResourcesApplicationFolder() {
        return getApplicationFolder();
    }

    private Path getComputedApplicationProperties() {
        return getResourcesApplicationFolder().resolve(APPLICATION_PROPERTIES);
    }

    private void createComputedApplicationProperties() {
        Path applicationProperties = getComputedApplicationProperties();
        Map<String, String> map = new HashMap<>();
        // Put the original application properties
        if (Files.exists(applicationProperties)) {
            map.putAll(PropertiesUtils.toMap(applicationProperties));
        }

        // Then put the build properties
        map.putAll(context.getOwner().getProperties());
        // Then replace the application properties
        PropertiesUtils.fromMap(map, applicationProperties);
    }

    private boolean isBuildProperty(String name) {
        return BUILD_PROPERTIES.stream().anyMatch(
                build -> name.matches(build) // It's a regular expression
                        || (build.endsWith(".") && name.startsWith(build)) // contains with
                        || name.equals(build)); // or it's equal to
    }

    private void copyResourcesInFolderToAppFolder(Path folder) {
        try (Stream<Path> binariesFound = Files
                .find(folder, Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> !Files.isDirectory(path))) {
            binariesFound.forEach(path -> {
                File fileToCopy = path.toFile();

                Path source = folder.relativize(path).getParent();
                Path target = getResourcesApplicationFolder();
                if (source != null) {
                    // Resource is in a sub-folder:
                    target = target.resolve(source);
                    // Create subdirectories if necessary
                    target.toFile().mkdirs();
                }

                FileUtils.copyFileTo(fileToCopy, target);
            });
        } catch (IOException ex) {
            // ignored
        }
    }

    private String propertyWithProfile(String name) {
        return "%" + context.getScenarioContext().getRunningTestClassName() + "." + name;
    }

}
