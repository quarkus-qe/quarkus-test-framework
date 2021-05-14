package io.quarkus.test.services.quarkus;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    private static final String BUILD_TIME_PROPERTIES = "/build-time-list";
    private static final String NATIVE_RUNNER = "-runner";
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app/";
    private static final String QUARKUS_RUN = "quarkus-run.jar";
    private static final String RESOURCES_FOLDER = "src/main/resources";
    private static final String TEST_RESOURCES_FOLDER = "src/test/resources";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final List<String> RESOURCES_TO_COPY = Arrays.asList(".sql", ".keystore", ".truststore");
    private static final Set<String> BUILD_PROPERTIES = FileUtils.loadFile(BUILD_TIME_PROPERTIES).lines().collect(toSet());
    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private Class<?>[] appClasses;
    private ServiceContext context;
    private LaunchMode launchMode = LaunchMode.JVM;
    private Path artifact;
    private boolean sslEnabled = false;
    private boolean selectedAppClasses = true;
    private QuarkusManagedResource managedResource;
    private Map<String, String> propertiesSnapshot;
    private String sourceS2iRepositoryUri;
    private String sourceS2iGitRef;
    private String sourceS2iContextDir;
    private String[] sourceS2iEnvVars;
    private Map<String, String> envVars;

    protected LaunchMode getLaunchMode() {
        return launchMode;
    }

    protected Path getArtifact() {
        return artifact;
    }

    protected ServiceContext getContext() {
        return context;
    }

    protected Class<?>[] getAppClasses() {
        return appClasses;
    }

    protected boolean isSelectedAppClasses() {
        return selectedAppClasses;
    }

    protected boolean isSslEnabled() {
        return sslEnabled;
    }

    protected String getSourceS2iRepositoryUri() {
        return sourceS2iRepositoryUri;
    }

    protected String getSourceS2iGitRef() {
        return sourceS2iGitRef;
    }

    protected String[] getSourceS2iEnvVars() {
        return sourceS2iEnvVars;
    }

    protected String getSourceS2iContextDir() {
        return sourceS2iContextDir;
    }

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
        sslEnabled = metadata.ssl();
        appClasses = metadata.classes();
        if (appClasses.length == 0) {
            appClasses = ClassPathUtils.findAllClassesFromSource();
            selectedAppClasses = false;
        }
        sourceS2iRepositoryUri = metadata.sourceS2iRepositoryUri();
        sourceS2iGitRef = metadata.sourceS2iGitRef();
        sourceS2iContextDir = metadata.sourceS2iContextDir();
        sourceS2iEnvVars = metadata.sourceS2iEnvVars();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        configureLogging();
        managedResource = findManagedResource();
        build();

        managedResource.validate();

        return managedResource;
    }

    public void build() {
        detectLaunchMode();
        if (managedResource.needsBuildArtifact()) {
            tryToReuseOrBuildArtifact();
        }
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
        return new HashMap<>(context.getOwner().getProperties());
    }

    public Map<String, String> getBuildProperties() {
        return getContext().getOwner().getProperties().entrySet().stream()
                .filter(e -> isBuildProperty(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isBuildProperty(String name) {
        return BUILD_PROPERTIES.stream().anyMatch(build -> name.matches(build) || name.startsWith(build));
    }

    private QuarkusManagedResource findManagedResource() {
        for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(context)) {
                return binding.init(this);
            }
        }

        return new LocalhostQuarkusApplicationManagedResource(this);
    }

    private void configureLogging() {
        context.getOwner().withProperty("quarkus.log.console.format", "%d{HH:mm:ss,SSS} %s%e%n");
    }

    private void tryToReuseOrBuildArtifact() {
        Optional<String> artifactLocation = Optional.empty();
        if (!containsBuildProperties() && !selectedAppClasses) {
            if (QuarkusProperties.isNativePackageType(context)) {
                artifactLocation = FileUtils.findTargetFile(NATIVE_RUNNER);
            } else {
                artifactLocation = FileUtils.findTargetFile(JVM_RUNNER)
                        .or(() -> FileUtils.findTargetFile(QUARKUS_APP, QUARKUS_RUN));
            }
        }

        if (artifactLocation.isEmpty()) {
            this.artifact = buildArtifact();
        } else {
            this.artifact = Path.of(artifactLocation.get());
        }
    }

    private Path buildArtifact() {
        try {
            Path appFolder = context.getServiceFolder();
            JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(appClasses);
            javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

            copyResourcesToAppFolder();

            Path testLocation = PathTestHelper.getTestClassesLocation(context.getTestContext().getRequiredTestClass());
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
                    .setMode(QuarkusBootstrap.Mode.PROD).setLocalProjectDiscovery(true).addExcludedPath(testLocation)
                    .setProjectRoot(testLocation).setBaseName(context.getName())
                    .setTargetDirectory(appFolder);

            AugmentResult result;
            try (CuratedApplication curatedApplication = builder.build().bootstrap()) {
                AugmentAction action = curatedApplication.createAugmentor();

                result = action.createProductionApplication();
            }

            return Optional.ofNullable(result.getNativeResult())
                    .orElseGet(() -> result.getJar().getPath());
        } catch (Exception ex) {
            fail("Failed to build Quarkus artifacts. Caused by " + ex);
        }

        return null;
    }

    private void detectLaunchMode() {
        if (QuarkusProperties.isNativePackageType(context)) {
            launchMode = LaunchMode.NATIVE;
        } else if (QuarkusProperties.isLegacyJarPackageType(context)) {
            launchMode = LaunchMode.LEGACY_JAR;
        } else {
            launchMode = LaunchMode.JVM;
        }
    }

    private void copyResourcesToAppFolder() throws IOException {
        copyResourcesInFolderToAppFolder(RESOURCES_FOLDER);
        copyResourcesInFolderToAppFolder(TEST_RESOURCES_FOLDER);
        PropertiesUtils.fromMap(createSnapshotOfBuildProperties(),
                context.getServiceFolder().resolve(APPLICATION_PROPERTIES));
    }

    private void copyResourcesInFolderToAppFolder(String folder) {
        try (Stream<Path> binariesFound = Files
                .find(Paths.get(folder), Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> RESOURCES_TO_COPY.stream()
                                .anyMatch(filter -> path.toFile().getName().endsWith(filter)))) {
            binariesFound.forEach(path -> FileUtils.copyFileTo(path.toFile(), context.getServiceFolder()));
        } catch (IOException ex) {
            // ignored
        }
    }

}
