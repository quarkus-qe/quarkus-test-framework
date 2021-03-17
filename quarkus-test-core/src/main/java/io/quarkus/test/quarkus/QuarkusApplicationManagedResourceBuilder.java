package io.quarkus.test.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.ManagedResource;
import io.quarkus.test.ManagedResourceBuilder;
import io.quarkus.test.NativeTest;
import io.quarkus.test.ServiceContext;
import io.quarkus.test.annotation.QuarkusApplication;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;

public class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    private static final String BUILD_TIME_PROPERTIES = "build-time-list.properties";
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
    private Path artifact;
    private boolean selectedAppClasses = true;

    protected Path getArtifact() {
        return artifact;
    }

    protected ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
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
        tryToReuseArtifact();

        for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(context)) {
                return binding.init(this, context);
            }
        }

        return new LocalhostQuarkusApplicationManagedResource(this);
    }

    private void configureLogging() {
        context.getOwner().withProperty("quarkus.log.console.format", "%d{HH:mm:ss,SSS} %s%e%n");
    }

    private boolean containsBuildProperties() {
        Set<String> properties = context.getOwner().getProperties().keySet();
        if (properties.isEmpty()) {
            return false;
        }

        Set<String> buildTimeProperties = listOfBuildTimeProperties();
        return properties.stream().anyMatch(prop -> buildTimeProperties.contains(prop));
    }

    private void tryToReuseArtifact() {
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
    }

    private Path buildArtifact() {
        try {
            Path appFolder = context.getServiceFolder();
            JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(appClasses);

            javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

            Properties buildProperties = new Properties();
            if (isNativeTest()) {
                buildProperties.put(QUARKUS_PACKAGE_TYPE_PROPERTY, NATIVE);
            }

            Path testLocation = PathTestHelper.getTestClassesLocation(context.getTestContext().getRequiredTestClass());
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
                    .setMode(QuarkusBootstrap.Mode.PROD).setLocalProjectDiscovery(true).addExcludedPath(testLocation)
                    .setProjectRoot(testLocation).setBaseName(context.getOwner().getName())
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

    private boolean isNativeTest() {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(NativeTest.class);
    }

    private static final Set<String> listOfBuildTimeProperties() {
        Set<String> buildTimeProperties = new HashSet<>();

        try (InputStream input = QuarkusApplicationManagedResourceBuilder.class.getClassLoader()
                .getResourceAsStream(BUILD_TIME_PROPERTIES)) {

            Properties prop = new Properties();
            prop.load(input);
            for (Entry<Object, Object> entry : prop.entrySet()) {
                buildTimeProperties.add((String) entry.getKey());
            }

        } catch (Exception ex) {
            fail("We could not load build time properties. Caused by " + ex);
        }

        return buildTimeProperties;
    }

}
