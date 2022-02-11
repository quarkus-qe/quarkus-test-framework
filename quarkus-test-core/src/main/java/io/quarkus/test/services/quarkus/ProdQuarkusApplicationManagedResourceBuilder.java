package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.FileUtils.findTargetFile;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.ReflectionUtils;

public class ProdQuarkusApplicationManagedResourceBuilder extends ArtifactQuarkusApplicationManagedResourceBuilder {

    protected static final String TARGET = "target";

    private static final String NATIVE_RUNNER = "-runner";
    private static final String EXE = ".exe";
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app";
    private static final String QUARKUS_RUN = "quarkus-run.jar";

    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private Path artifact;
    private QuarkusManagedResource managedResource;

    @Override
    protected Path getArtifact() {
        return artifact;
    }

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
        setPropertiesFile(metadata.properties());
        setSslEnabled(metadata.ssl());
        setGrpcEnabled(metadata.grpc());
        initAppClasses(metadata.classes());
        initForcedDependencies(metadata.dependencies());
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
        managedResource = findManagedResource();
        build();

        return managedResource;
    }

    public void build() {
        managedResource.onPreBuild();
        copyResourcesToAppFolder();
        if (managedResource.needsBuildArtifact()) {
            tryToReuseOrBuildArtifact();
        }

        managedResource.onPostBuild();
    }

    protected QuarkusManagedResource findManagedResource() {
        for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        return new ProdLocalhostQuarkusApplicationManagedResource(this);
    }

    protected Path getTargetFolderForLocalArtifacts() {
        return Paths.get(TARGET);
    }

    private void tryToReuseOrBuildArtifact() {
        Optional<String> artifactLocation = Optional.empty();
        if (!containsBuildProperties() && !requiresCustomBuild()) {
            if (QuarkusProperties.isNativePackageType(getContext())) {
                String nativeRunnerExpectedLocation = NATIVE_RUNNER;
                if (OS.WINDOWS.isCurrentOs()) {
                    nativeRunnerExpectedLocation += EXE;
                }

                artifactLocation = findTargetFile(getTargetFolderForLocalArtifacts(), nativeRunnerExpectedLocation);

            } else {
                artifactLocation = findTargetFile(getTargetFolderForLocalArtifacts(), JVM_RUNNER)
                        .or(() -> findTargetFile(getTargetFolderForLocalArtifacts().resolve(QUARKUS_APP), QUARKUS_RUN));
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
            createSnapshotOfBuildProperties();
            Path appFolder = getApplicationFolder();

            JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(getAppClasses());
            javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

            Path testLocation = PathTestHelper.getTestClassesLocation(getContext().getTestContext().getRequiredTestClass());
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
                    .setMode(QuarkusBootstrap.Mode.PROD)
                    .addExcludedPath(testLocation)
                    .setIsolateDeployment(true)
                    .setProjectRoot(testLocation)
                    .setBaseName(getContext().getName())
                    .setTargetDirectory(appFolder);

            if (!getForcedDependencies().isEmpty()) {
                // The method setForcedDependencies signature changed from `List<AppDependency>` to `List<Dependency>` where
                // Dependency is an interface of AppDependency, so it should be fine. However, the compiler fails to cast it,
                // so we need to use reflection to sort it out for the most recent version and older versions.
                ReflectionUtils.invokeMethod(builder, "setForcedDependencies", getForcedDependencies());
            }

            // The method `setLocalProjectDiscovery` signature changed from `Boolean` to `boolean` and this might make
            // to fail the tests at runtime when using different versions.
            // In order to workaround this, we need to invoke this method at runtime to let JVM unbox the arguments properly.
            // Note that this is happening because we want to support both 2.x and 1.13.x Quarkus versions.
            // Another strategy could be to have our own version of Quarkus bootstrap.
            ReflectionUtils.invokeMethod(builder, "setLocalProjectDiscovery", true);

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

}
