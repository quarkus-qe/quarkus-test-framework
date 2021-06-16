package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;

public class ProdQuarkusApplicationManagedResourceBuilder extends QuarkusApplicationManagedResourceBuilder {

    private static final String NATIVE_RUNNER = "-runner";
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app/";
    private static final String QUARKUS_RUN = "quarkus-run.jar";

    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private LaunchMode launchMode = LaunchMode.JVM;
    private Path artifact;
    private QuarkusManagedResource managedResource;

    protected LaunchMode getLaunchMode() {
        return launchMode;
    }

    protected Path getArtifact() {
        return artifact;
    }

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
        setSslEnabled(metadata.ssl());
        initAppClasses(metadata.classes());
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
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

    private QuarkusManagedResource findManagedResource() {
        for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        return new ProdLocalhostQuarkusApplicationManagedResource(this);
    }

    private void tryToReuseOrBuildArtifact() {
        Optional<String> artifactLocation = Optional.empty();
        if (!containsBuildProperties() && !isSelectedAppClasses()) {
            if (QuarkusProperties.isNativePackageType(getContext())) {
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
            Path appFolder = getContext().getServiceFolder();
            JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(getAppClasses());
            javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

            copyResourcesToAppFolder();

            Path testLocation = PathTestHelper.getTestClassesLocation(getContext().getTestContext().getRequiredTestClass());
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
                    .setMode(QuarkusBootstrap.Mode.PROD)
                    .setLocalProjectDiscovery(true)
                    .addExcludedPath(testLocation)
                    .setProjectRoot(testLocation)
                    .setBaseName(getContext().getName())
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
        if (QuarkusProperties.isNativePackageType(getContext())) {
            launchMode = LaunchMode.NATIVE;
        } else if (QuarkusProperties.isLegacyJarPackageType(getContext())) {
            launchMode = LaunchMode.LEGACY_JAR;
        } else {
            launchMode = LaunchMode.JVM;
        }
    }

}
