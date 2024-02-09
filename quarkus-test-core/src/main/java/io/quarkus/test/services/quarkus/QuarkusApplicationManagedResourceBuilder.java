package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.PropertiesUtils.resolveProperty;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.builder.Version;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;

public abstract class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    public static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    public static final String QUARKUS_GRPC_SERVER_PORT_PROPERTY = "quarkus.grpc.server.port";
    public static final String QUARKUS_HTTP_SSL_PORT_PROPERTY = "quarkus.http.ssl-port";
    public static final int HTTP_PORT_DEFAULT = 8080;
    public static final int MANAGEMENT_PORT_DEFAULT = 9000;

    protected static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources");

    private static final String BUILD_TIME_PROPERTIES = "/build-time-list";
    private static final Path TEST_RESOURCES_FOLDER = Paths.get("src", "test", "resources");
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final Set<String> BUILD_PROPERTIES = FileUtils.loadFile(BUILD_TIME_PROPERTIES).lines().collect(toSet());
    private static final String DEPENDENCY_SCOPE_DEFAULT = "compile";
    private static final String QUARKUS_GROUP_ID_DEFAULT = "io.quarkus";
    private static final int DEPENDENCY_DIRECT_FLAG = 0b000010;

    private Class<?>[] appClasses;
    /**
     * Whether build consist of all source classes or only some of them.
     */
    private boolean buildWithAllClasses = true;
    private List<AppDependency> forcedDependencies = Collections.emptyList();
    private boolean requiresCustomBuild = false;
    private ServiceContext context;
    private String propertiesFile = APPLICATION_PROPERTIES;
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

    protected void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
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

    protected boolean isBuildWithAllClasses() {
        return buildWithAllClasses;
    }

    protected List<AppDependency> getForcedDependencies() {
        return forcedDependencies;
    }

    protected boolean requiresCustomBuild() {
        return requiresCustomBuild;
    }

    @Override
    public String getComputedProperty(String name) {
        Path applicationProperties = getComputedApplicationProperties();
        if (!Files.exists(applicationProperties)) {
            // computed properties have not been propagated yet, we use the one from src/main/resources
            applicationProperties = RESOURCES_FOLDER.resolve(propertiesFile);
        }

        if (!Files.exists(applicationProperties)) {
            return null;
        }

        Map<String, String> computedProperties = PropertiesUtils.toMap(applicationProperties);
        return Optional.ofNullable(computedProperties.get(name))
                .orElseGet(() -> computedProperties.get(propertyWithProfile(name)));
    }

    public boolean buildPropertiesChanged() {
        Map<String, String> differenceProperties = MapUtils.difference(context.getOwner().getProperties(), propertiesSnapshot);
        Set<String> properties = differenceProperties.keySet();
        if (properties.isEmpty()) {
            return false;
        }

        return properties.stream().anyMatch(this::isBuildProperty);
    }

    public boolean containsBuildProperties() {
        if (propertiesSnapshot == null) {
            return false;
        }
        return propertiesSnapshot.keySet().stream().anyMatch(this::isBuildProperty);
    }

    /**
     * Creates snapshot of build properties if it doesn't exist and determines whether custom build is required
     * due to build-time properties in native mode.
     */
    public void createSnapshotOfBuildPropertiesIfNotExists() {
        if (propertiesSnapshot == null) {
            createSnapshotOfBuildProperties();
        }
    }

    public boolean hasAppSpecificConfigProperties() {
        return propertiesSnapshot != null && !propertiesSnapshot.isEmpty();
    }

    public Map<String, String> createSnapshotOfBuildProperties() {
        propertiesSnapshot = new HashMap<>(context.getOwner().getProperties());

        // if there are build-time properties, custom build is required
        if (!requiresCustomBuild) {
            requiresCustomBuild = containsBuildProperties();
        }

        return propertiesSnapshot;
    }

    public Map<String, String> getBuildProperties() {
        return getContext().getOwner().getProperties().entrySet().stream()
                .filter(e -> isBuildProperty(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Set<String> getBuildPropertiesSetAsSystemProperties() {
        // the idea is to collect properties set as -Dquarkus.property-key=value
        // this can be done more robust with environment properties etc. if necessary
        return System.getProperties()
                .entrySet()
                .stream()
                .filter(e -> e.getKey() instanceof String)
                .filter(e -> isBuildProperty((String) e.getKey()))
                .map(e -> PropertiesUtils.toMvnSystemProperty((String) e.getKey(), (String) e.getValue()))
                .collect(toSet());
    }

    public void initAppClasses(Class<?>[] classes) {
        requiresCustomBuild = true;
        appClasses = classes;
        if (appClasses == null || appClasses.length == 0) {
            appClasses = ClassPathUtils.findAllClassesFromSource();
            requiresCustomBuild = false;
        } else {
            buildWithAllClasses = false;
        }
    }

    public void initForcedDependencies(Dependency[] forcedDependencies) {
        if (forcedDependencies != null && forcedDependencies.length > 0) {
            requiresCustomBuild = true;
            this.forcedDependencies = Stream.of(forcedDependencies).map(d -> {
                String groupId = StringUtils.defaultIfEmpty(resolveProperty(d.groupId()), QUARKUS_GROUP_ID_DEFAULT);
                String version = StringUtils.defaultIfEmpty(resolveProperty(d.version()), Version.getVersion());
                AppArtifact artifact = new AppArtifact(groupId, d.artifactId(), version);
                return new AppDependency(artifact, DEPENDENCY_SCOPE_DEFAULT, DEPENDENCY_DIRECT_FLAG);
            }).collect(Collectors.toList());
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

    protected Path getComputedApplicationProperties() {
        return getResourcesApplicationFolder().resolve(APPLICATION_PROPERTIES);
    }

    private void createComputedApplicationProperties() {
        Path sourceApplicationProperties = getResourcesApplicationFolder().resolve(propertiesFile);
        Path generatedApplicationProperties = getResourcesApplicationFolder().resolve(APPLICATION_PROPERTIES);
        Map<String, String> map = new HashMap<>();
        // Add the content of the source application properties into the auto-generated application.properties
        if (Files.exists(sourceApplicationProperties)) {
            map.putAll(PropertiesUtils.toMap(sourceApplicationProperties));
        }

        // Then add the service properties
        map.putAll(context.getOwner().getProperties());
        // Then overwrite the application properties with the generated application.properties
        PropertiesUtils.fromMap(map, generatedApplicationProperties);
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

    private boolean isQuarkusVersion2Dot3OrAbove() {
        String quarkusVersion = QuarkusProperties.getVersion();
        return !quarkusVersion.startsWith("2.2.")
                && !quarkusVersion.startsWith("2.1.")
                && !quarkusVersion.startsWith("2.0.")
                && !quarkusVersion.startsWith("1.");
    }

    public boolean useSeparateManagementInterface() {
        return getContext().getOwner().getProperty("quarkus.management.enabled")
                .map("true"::equals)
                .orElse(false);
    }

    public int getManagementPort() {
        if (useSeparateManagementInterface()) {
            return getPort("quarkus.management.port").orElse(MANAGEMENT_PORT_DEFAULT);
        }
        return getHttpPort();
    }

    public boolean useManagementSsl() {
        return getContext().getOwner().getProperty("quarkus.management.ssl.certificate.key-store-file").isPresent();
    }

    public int getHttpPort() {
        return getPort(QUARKUS_HTTP_PORT_PROPERTY).orElse(HTTP_PORT_DEFAULT);
    }

    private Optional<Integer> getPort(String property) {
        return getContext().getOwner().getProperty(property)
                .map(Integer::parseInt);
    }
}
