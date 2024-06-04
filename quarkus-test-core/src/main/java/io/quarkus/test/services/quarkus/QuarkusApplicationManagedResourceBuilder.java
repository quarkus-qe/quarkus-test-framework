package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.PropertiesUtils.resolveProperty;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.builder.Version;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.quarkus.test.utils.TestExecutionProperties;

public abstract class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    public static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    public static final String QUARKUS_GRPC_SERVER_PORT_PROPERTY = "quarkus.grpc.server.port";
    public static final String QUARKUS_HTTP_SSL_PORT_PROPERTY = "quarkus.http.ssl-port";
    public static final int HTTP_PORT_DEFAULT = 8080;
    public static final int MANAGEMENT_PORT_DEFAULT = 9000;

    protected static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources");

    private static final Path TEST_RESOURCES_FOLDER = Paths.get("src", "test", "resources");
    private static final String APPLICATION_PROPERTIES = "application.properties";
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
    private CertificateBuilder certificateBuilder;
    private Set<String> detectedBuildTimeProperties;

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

    protected void configureCertificates() {
        if (certificateBuilder != null) {
            getContext().put(CertificateBuilder.INSTANCE_KEY, certificateBuilder);
            certificateBuilder
                    .certificates()
                    .forEach(certificate -> certificate
                            .configProperties()
                            .forEach((k, v) -> getContext().withTestScopeConfigProperty(k, v)));
        }
    }

    protected void setCertificateBuilder(CertificateBuilder certificateBuilder) {
        this.certificateBuilder = certificateBuilder;
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

        // detect build time properties in computed properties and other config sources
        detectBuildTimeProperties(map);
    }

    protected void detectBuildTimeProperties(Map<String, String> computedProperties) {
        // this won't detect build-time properties for dependencies forced with @Dependency as they are not
        // on the classpath which doesn't matter because custom build is always required when any dependency is forced
        var classLoader = Thread.currentThread().getContextClassLoader();

        try {
            var buildTimeConfigReader = new BuildTimeConfigurationReader(classLoader);
            var buildSystemProps = new Properties();
            buildSystemProps.putAll(computedProperties);

            // this must always be set as Quarkus sets and config expansions would fail
            buildSystemProps.put("platform.quarkus.native.builder-image", "<<ignored>>");

            var config = buildTimeConfigReader.initConfiguration(LaunchMode.NORMAL, buildSystemProps, Map.of());
            var readResult = buildTimeConfigReader.readConfiguration(config);
            var buildTimeConfigKeys = new HashSet<String>();
            buildTimeConfigKeys.addAll(readResult.getAllBuildTimeValues().keySet());
            buildTimeConfigKeys.addAll(readResult.getBuildTimeRunTimeValues().keySet());

            // gather build-time config keys from extensions deployment modules
            // this won't work for named config keys (without regex), but we can tweak that in the future if we need to
            var deploymentBuildProps = Arrays
                    .stream(FileUtils
                            .loadFile("/deployment-build-props.txt")
                            .split(System.lineSeparator()))
                    .map(String::trim)
                    .collect(toSet());
            buildTimeConfigKeys.addAll(deploymentBuildProps);

            // handle relocations - if relocation processor is applied, we won't find original config property
            // in the build-time or build-time-runtime-fixed properties as we can only find there the relocated one
            var relocatedBuildTimeProps = buildSystemProps
                    .stringPropertyNames()
                    .stream()
                    .filter(p -> !buildTimeConfigKeys.contains(p))
                    .filter(p -> !p.equals(config.getConfigValue(p).getName()))
                    .filter(p -> buildTimeConfigKeys.contains(config.getConfigValue(p).getName()))
                    .collect(toSet());
            buildTimeConfigKeys.addAll(relocatedBuildTimeProps);

            this.detectedBuildTimeProperties = Set.copyOf(buildTimeConfigKeys);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to detect build time properties", e);
        }
    }

    private Set<String> getDetectedBuildTimeProperties() {
        if (detectedBuildTimeProperties == null) {
            throw new IllegalStateException("""
                    Build time configuration properties are not initialized yet.
                    This must mean there is a race in the Test Framework design and we need to fix it.
                    """);
        }
        return detectedBuildTimeProperties;
    }

    private boolean isBuildProperty(String name) {
        return getDetectedBuildTimeProperties().contains(name);
    }

    protected void copyResourcesInFolderToAppFolder(Path folder) {
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
        return TestExecutionProperties.useManagementSsl(getContext().getOwner());
    }

    public int getHttpPort() {
        return getPort(QUARKUS_HTTP_PORT_PROPERTY).orElse(HTTP_PORT_DEFAULT);
    }

    private Optional<Integer> getPort(String property) {
        return getContext().getOwner().getProperty(property)
                .map(Integer::parseInt);
    }
}
