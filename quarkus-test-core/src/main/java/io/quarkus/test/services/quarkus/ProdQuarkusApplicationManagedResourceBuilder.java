package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.FileUtils.findTargetFile;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;
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
import io.quarkus.test.utils.Command;

public class ProdQuarkusApplicationManagedResourceBuilder extends ArtifactQuarkusApplicationManagedResourceBuilder {

    static final String NATIVE_RUNNER = "-runner";
    static final String EXE = ".exe";
    static final String TARGET = "target";

    private static final Logger LOG = Logger.getLogger(ProdQuarkusApplicationManagedResourceBuilder.class.getName());
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app";
    private static final String QUARKUS_RUN = "quarkus-run.jar";

    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private Path artifact;
    private QuarkusManagedResource managedResource;
    private String artifactSuffix;

    @Override
    protected Path getArtifact() {
        return artifact;
    }

    protected void setArtifactSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty() || suffix.isBlank()) {
            this.artifactSuffix = null;
        } else {
            this.artifactSuffix = suffix;
        }
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
            try {
                this.artifact = tryToReuseOrBuildArtifact();
            } catch (Throwable t) {
                debugFileAccessRaceOnWindows();
                throw t;
            }
        }

        managedResource.onPostBuild();
    }

    private void debugFileAccessRaceOnWindows() {
        if (OS.current() == OS.WINDOWS && Boolean.getBoolean("enable-win-race-debug")) {
            LOG.info("List all running processes:");
            ProcessHandle.allProcesses().forEach(processHandle -> {
                LOG.infof("pid - '%s', is alive - '%b', command - '%s', arguments - '%s'", processHandle.pid(),
                        processHandle.isAlive(), processHandle.info().command().orElse(""),
                        processHandle.info().arguments());
            });
            LOG.info("Print all file open handles:");
            try {
                new Command(".\\handle.exe -accepteula").onDirectory(Path.of(System.getenv("GITHUB_WORKSPACE"))).runAndWait();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to run 'handle.exe': " + e.getMessage());
            }
        }
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

    private Path tryToReuseOrBuildArtifact() {
        Optional<String> artifactLocation = Optional.empty();
        final Path targetFolder = getTargetFolderForLocalArtifacts();
        if (artifactSuffix != null) {
            return findTargetFile(targetFolder, artifactSuffix)
                    .map(Path::of)
                    .orElseThrow(() -> new IllegalStateException(String.format("Folder %s doesn't contain '%s'",
                            targetFolder,
                            artifactSuffix)));
        }
        if (!containsBuildProperties() && !requiresCustomBuild()) {
            if (QuarkusProperties.isNativePackageType(getContext())) {
                String nativeRunnerExpectedLocation = NATIVE_RUNNER;
                if (OS.WINDOWS.isCurrentOs()) {
                    nativeRunnerExpectedLocation += EXE;
                }

                artifactLocation = findTargetFile(targetFolder, nativeRunnerExpectedLocation);
            } else {
                artifactLocation = findTargetFile(targetFolder, JVM_RUNNER)
                        .or(() -> findTargetFile(targetFolder.resolve(QUARKUS_APP), QUARKUS_RUN));
            }
        }

        if (artifactLocation.isEmpty()) {
            return buildArtifact();
        } else {
            return Path.of(artifactLocation.get());
        }
    }

    private Path buildArtifact() {
        if (QuarkusProperties.isNativePackageType(getContext()) && OS.WINDOWS.isCurrentOs()) {
            return new QuarkusMavenPluginBuildHelper(this).buildNativeExecutable();
        }
        return buildArtifactUsingQuarkusBootstrap();
    }

    private Path buildArtifactUsingQuarkusBootstrap() {
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
                    .setTargetDirectory(appFolder)
                    .setLocalProjectDiscovery(true);

            if (!getForcedDependencies().isEmpty()) {
                builder.setForcedDependencies(new ArrayList<>(getForcedDependencies()));
            }

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
