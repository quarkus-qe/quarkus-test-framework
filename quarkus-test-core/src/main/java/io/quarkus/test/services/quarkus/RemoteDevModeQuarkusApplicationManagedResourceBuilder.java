package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.createDisableBuildAnalyticsProperty;
import static io.quarkus.test.utils.FileUtils.findTargetFile;
import static io.quarkus.test.utils.MavenUtils.ENSURE_QUARKUS_BUILD;
import static io.quarkus.test.utils.MavenUtils.SKIP_CHECKSTYLE;
import static io.quarkus.test.utils.MavenUtils.SKIP_ITS;
import static io.quarkus.test.utils.MavenUtils.SKIP_TESTS;
import static io.quarkus.test.utils.MavenUtils.installParentPomsIfNeeded;
import static io.quarkus.test.utils.MavenUtils.withProperty;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.RemoteDevModeQuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MavenUtils;
import io.quarkus.test.utils.ProcessBuilderProvider;

public class RemoteDevModeQuarkusApplicationManagedResourceBuilder extends ArtifactQuarkusApplicationManagedResourceBuilder {

    public static final String QUARKUS_LIVE_RELOAD_PASSWORD = "quarkus.live-reload.password";
    public static final String QUARKUS_LAUNCH_DEV_MODE = "QUARKUS_LAUNCH_DEVMODE";
    public static final String EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON = "Connected to remote server";

    private static final String QUARKUS_LIVE_RELOAD_URL = "quarkus.live-reload.url";
    private static final String QUARKUS_APP = "quarkus-app";
    private static final String QUARKUS_RUN = "quarkus-run.jar";
    private static final String TARGET = "target";
    private static final String RUNNER = "runner";

    private final ServiceLoader<RemoteDevModeQuarkusApplicationManagedResourceBinding> bindings = ServiceLoader
            .load(RemoteDevModeQuarkusApplicationManagedResourceBinding.class);

    private String liveReloadPassword;
    private Path artifact;
    private QuarkusManagedResource managedResource;

    @Override
    public void init(Annotation annotation) {
        RemoteDevModeQuarkusApplication metadata = (RemoteDevModeQuarkusApplication) annotation;
        liveReloadPassword = metadata.password();
        setPropertiesFile(metadata.properties());
        initAppClasses(new Class<?>[0]);
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
        managedResource = findManagedResource();
        build();

        return managedResource;
    }

    @Override
    protected void build() {
        try {
            new QuarkusMavenPluginBuildHelper(this).prepareApplicationFolder();

            // Create mutable jar
            installParentPomsIfNeeded();
            MavenUtils.build(getContext(), Arrays.asList(SKIP_ITS, SKIP_TESTS, SKIP_CHECKSTYLE, ENSURE_QUARKUS_BUILD,
                    withProperty(QuarkusProperties.PACKAGE_TYPE_NAME, QuarkusProperties.MUTABLE_JAR)));

            // Move artifact to an isolated location
            FileUtils.copyDirectoryTo(getContext().getServiceFolder().resolve(TARGET),
                    getContext().getServiceFolder().resolve(RUNNER));

            // Locate artifacts
            Path target = getContext().getServiceFolder().resolve(RUNNER).resolve(QUARKUS_APP);
            Optional<String> artifactLocation = findTargetFile(target, QUARKUS_RUN);
            if (artifactLocation.isEmpty()) {
                fail("Quarkus runner could not be found for mutable-jar type");
            }

            this.artifact = Path.of(artifactLocation.get());
        } catch (Exception ex) {
            fail("Failed to build Quarkus artifacts. Caused by " + ex);
        }
    }

    @Override
    protected Path getResourcesApplicationFolder() {
        return super.getResourcesApplicationFolder().resolve(RESOURCES_FOLDER);
    }

    @Override
    protected Path getArtifact() {
        return artifact;
    }

    protected String getLiveReloadPassword() {
        return liveReloadPassword;
    }

    protected QuarkusManagedResource findManagedResource() {
        for (RemoteDevModeQuarkusApplicationManagedResourceBinding binding : bindings) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        return new RemoteDevModeLocalhostQuarkusApplicationManagedResource(this);
    }

    protected ProcessBuilder prepareRemoteDevProcess() {
        Log.info("Running Remote Dev daemon to watch changes");

        List<String> command = MavenUtils.mvnCommand(getContext());
        command.add(withProperty(QuarkusProperties.PACKAGE_TYPE_NAME, QuarkusProperties.MUTABLE_JAR));
        command.add(withProperty(QUARKUS_LIVE_RELOAD_PASSWORD, liveReloadPassword));
        command.add(withProperty(QUARKUS_LIVE_RELOAD_URL,
                managedResource.getURI(Protocol.HTTP).toString()));
        command.add("quarkus:remote-dev");
        if (QuarkusProperties.disableBuildAnalytics()) {
            command.add(createDisableBuildAnalyticsProperty());
        }

        Log.info("Running command: %s", String.join(" ", command));

        return ProcessBuilderProvider.command(command)
                .redirectErrorStream(true)
                .directory(getApplicationFolder().toFile());
    }
}
