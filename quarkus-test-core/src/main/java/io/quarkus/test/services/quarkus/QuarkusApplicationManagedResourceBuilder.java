package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;
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
import io.quarkus.test.scenarios.NativeScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;

public class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    private static final String BUILD_TIME_PROPERTIES = "/build-time-list";
    private static final String NATIVE_RUNNER = "-runner";
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app/";
    private static final String QUARKUS_RUN = "quarkus-run.jar";

    private static final String QUARKUS_PACKAGE_TYPE_PROPERTY = "quarkus.package.type";
    private static final String NATIVE = "native";

    private Class<?>[] appClasses;

    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private ServiceContext context;
    private LaunchMode launchMode = LaunchMode.JVM;
    private Path artifact;
    private boolean sslEnabled = false;
    private boolean selectedAppClasses = true;
    private QuarkusManagedResource managedResource;
    private Map<String, String> propertiesSnapshot;

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

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
        sslEnabled = metadata.ssl();
        appClasses = metadata.classes();
        if (appClasses.length == 0) {
            appClasses = ClassPathUtils.findAllClassesFromSource();
            selectedAppClasses = false;
        }
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

        Set<String> buildTimeProperties = listOfBuildTimeProperties();
        return properties.stream().anyMatch(prop -> buildTimeProperties.stream().anyMatch(prop::matches));
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
            if (isNativeTest()) {
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

        detectLaunchMode();
    }

    private Path buildArtifact() {
        try {
            Path appFolder = context.getServiceFolder();
            JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(appClasses);
            javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

            Properties buildProperties = prepareBuildProperties();

            Path testLocation = PathTestHelper.getTestClassesLocation(context.getTestContext().getRequiredTestClass());
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
                    .setMode(QuarkusBootstrap.Mode.PROD).setLocalProjectDiscovery(true).addExcludedPath(testLocation)
                    .setProjectRoot(testLocation).setBaseName(context.getName())
                    .setBuildSystemProperties(buildProperties).setTargetDirectory(appFolder);

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

    private Properties prepareBuildProperties() {
        propertiesSnapshot = new HashMap<>(context.getOwner().getProperties());

        Properties buildProperties = new Properties();
        buildProperties.putAll(propertiesSnapshot);
        if (isNativeTest()) {
            buildProperties.put(QUARKUS_PACKAGE_TYPE_PROPERTY, NATIVE);
        } else if (StringUtils.equals(NATIVE, buildProperties.getProperty(QUARKUS_PACKAGE_TYPE_PROPERTY))) {
            // When using native profile, but running a JVM scenario.
            buildProperties.remove(QUARKUS_PACKAGE_TYPE_PROPERTY);
        }

        return buildProperties;
    }

    private void detectLaunchMode() {
        if (isNativeTest()) {
            launchMode = LaunchMode.NATIVE;
        } else if (artifact.endsWith(JVM_RUNNER)) {
            launchMode = LaunchMode.FAST_JAR;
        } else {
            launchMode = LaunchMode.JVM;
        }
    }

    private boolean isNativeTest() {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(NativeScenario.class);
    }

    private static Set<String> listOfBuildTimeProperties() {
        return FileUtils.loadFile(BUILD_TIME_PROPERTIES).lines().collect(Collectors.toSet());
    }

}
