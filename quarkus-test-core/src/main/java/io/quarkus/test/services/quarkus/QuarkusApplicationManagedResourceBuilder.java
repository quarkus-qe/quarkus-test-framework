package io.quarkus.test.services.quarkus;

import static io.quarkus.test.security.certificate.ServingCertificateConfig.SERVING_CERTIFICATE_KEY;
import static io.quarkus.test.utils.PropertiesUtils.resolveProperty;
import static io.quarkus.test.utils.TestExecutionProperties.isKubernetesPlatform;
import static io.quarkus.test.utils.TestExecutionProperties.isOpenshiftPlatform;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.quarkus.test.utils.TestExecutionProperties;
import io.smallrye.config.SecretKeys;

public abstract class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    public static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    public static final String QUARKUS_GRPC_SERVER_PORT_PROPERTY = "quarkus.grpc.server.port";
    public static final String QUARKUS_HTTP_SSL_PORT_PROPERTY = "quarkus.http.ssl-port";
    public static final int HTTP_PORT_DEFAULT = 8080;
    public static final int MANAGEMENT_PORT_DEFAULT = 9000;

    protected static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources");

    private static final Path TEST_RESOURCES_FOLDER = Paths.get("src", "test", "resources");
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String QUARKUS_GROUP_ID_DEFAULT = "io.quarkus";

    private Class<?>[] appClasses;
    /**
     * Whether build consist of all source classes or only some of them.
     */
    private boolean buildWithAllClasses = true;
    private List<io.quarkus.test.services.quarkus.Dependency> forcedDependencies = Collections.emptyList();
    private boolean requiresCustomBuild = false;
    private ServiceContext context;
    private String propertiesFile = APPLICATION_PROPERTIES;
    private boolean sslEnabled = false;
    private boolean grpcEnabled = false;
    private Map<String, String> propertiesSnapshot;
    private CertificateBuilder certificateBuilder;
    private Set<String> detectedBuildTimeProperties;
    private boolean needsEnhancedApplicationProperties = false;
    private boolean s2iScenario = false;

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

    protected List<io.quarkus.test.services.quarkus.Dependency> getForcedDependencies() {
        return forcedDependencies;
    }

    protected boolean requiresCustomBuild() {
        return requiresCustomBuild;
    }

    protected void setCustomBuildRequired() {
        this.requiresCustomBuild = true;
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

    protected void setS2iScenario() {
        this.s2iScenario = true;
    }

    public void initForcedDependencies(Dependency[] forcedDependencies) {
        if (forcedDependencies != null && forcedDependencies.length > 0) {
            requiresCustomBuild = true;
            this.forcedDependencies = Stream.of(forcedDependencies).map(d -> {
                String groupId = StringUtils.defaultIfEmpty(resolveProperty(d.groupId()), QUARKUS_GROUP_ID_DEFAULT);
                return new io.quarkus.test.services.quarkus.Dependency(groupId, d.artifactId(), getVersion(d));
            }).collect(Collectors.toList());
        }
    }

    private static String getVersion(Dependency dependency) {
        if (dependency.version() == null || dependency.version().isEmpty()) {
            return null;
        }
        return resolveProperty(dependency.version());
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
            if (certificateBuilder.servingCertificateConfig() != null) {
                getContext().put(SERVING_CERTIFICATE_KEY, certificateBuilder.servingCertificateConfig());
            }
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
        // this needs systematic change so that users understand what will end-up as application property and
        // what will end-up as system property
        Path sourceApplicationProperties = getResourcesApplicationFolder().resolve(propertiesFile);
        Path generatedApplicationProperties = getResourcesApplicationFolder().resolve(APPLICATION_PROPERTIES);
        Map<String, String> map = new HashMap<>();
        // Add the content of the source application properties into the auto-generated application.properties
        if (Files.exists(sourceApplicationProperties)) {
            var sourceAppPropsMap = PropertiesUtils.toMap(sourceApplicationProperties);
            if (!propertiesFile.equalsIgnoreCase(APPLICATION_PROPERTIES) && !sourceAppPropsMap.isEmpty()) {
                needsEnhancedApplicationProperties = true;
            }
            map.putAll(sourceAppPropsMap);
        } else if (!propertiesFile.equalsIgnoreCase(APPLICATION_PROPERTIES)) {
            // This branch means, that the user added custom file name, but the file doesn't exist.
            // I presume, that situation, where user explicitly adds default file name(ie `application.properties`)
            // but the file doesn't exist, is too rare to be considered seriously.
            throw new IllegalStateException("Requested properties file " + sourceApplicationProperties + " doesn't exist");
        }

        // Then add the service properties
        var svcContextProperties = context.getOwner().getProperties();

        var allProperties = new HashMap<>(map);
        allProperties.putAll(svcContextProperties);
        // detect build time properties in computed properties, service properties and other config sources
        detectBuildTimeProperties(allProperties);

        svcContextProperties.forEach((k, v) -> {
            // this needs rework, but it seems we consider as runtime properties everything added with 'withProperty'
            // unless it is detected as build property
            // we always include all the properties into application.properties in OCP and K8 because we can't rely on
            // system properties there
            if (isBuildProperty(k) || isOpenshiftPlatform() || isKubernetesPlatform()) {
                if (!needsEnhancedApplicationProperties) {
                    needsEnhancedApplicationProperties = true;
                }
                map.put(k, v);
            }
        });
        // Then overwrite the application properties with the generated application.properties
        PropertiesUtils.fromMap(map, generatedApplicationProperties);
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

            var config = buildTimeConfigReader.initConfiguration(buildSystemProps, new Properties(), Map.of());
            var readResult = buildTimeConfigReader.readConfiguration(config);
            var buildTimeConfigKeys = new HashSet<String>();
            buildTimeConfigKeys.addAll(readResult.getAllBuildTimeValues().keySet());
            buildTimeConfigKeys.addAll(readResult.getBuildTimeRunTimeValues().keySet());

            // gather build-time config keys from extensions deployment modules
            // this won't work for named config keys (without regex), but we can tweak that in the future if we need to
            var deploymentBuildProps = IOUtils
                    .readLines(QuarkusApplicationManagedResourceBuilder.class
                            .getResourceAsStream("/deployment-build-props.txt"), StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .collect(toSet());
            if (deploymentBuildProps.size() <= 1) {
                throw new RuntimeException("deployment-build-props.txt couldn't be properly loaded");
            }
            buildTimeConfigKeys.addAll(deploymentBuildProps);

            // handle relocations - if relocation processor is applied, we won't find original config property
            // in the build-time or build-time-runtime-fixed properties as we can only find there the relocated one;
            // we can safely guess that secret keys are not build-time config props,
            // but we don't create config ourselves, hence unlock keys to avoid build failures
            var relocatedBuildTimeProps = SecretKeys.doUnlocked(() -> buildSystemProps
                    .stringPropertyNames()
                    .stream()
                    .filter(p -> !buildTimeConfigKeys.contains(p))
                    .filter(p -> !p.equals(config.getConfigValue(p).getName()))
                    .filter(p -> buildTimeConfigKeys.contains(config.getConfigValue(p).getName()))
                    .collect(toSet()));
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

    protected boolean areApplicationPropertiesEnhanced() {
        return needsEnhancedApplicationProperties;
    }

    protected boolean isS2iScenario() {
        return s2iScenario;
    }
}
