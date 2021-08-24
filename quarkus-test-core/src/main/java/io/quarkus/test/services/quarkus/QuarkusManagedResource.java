package io.quarkus.test.services.quarkus;

import java.util.Arrays;
import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public abstract class QuarkusManagedResource implements ManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final List<String> ERRORS = Arrays.asList("Failed to start application",
            "Failed to load config value of type class",
            "Quarkus may already be running or the port is used by another application",
            "One or more configuration errors have prevented the application from starting",
            "Attempting to start live reload endpoint to recover from previous Quarkus startup failure");

    private final ServiceContext serviceContext;
    private final LaunchMode launchMode;

    public QuarkusManagedResource(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
        this.launchMode = detectLaunchMode(serviceContext);
    }

    @Override
    public String getDisplayName() {
        return String.format("Quarkus %s mode", getLaunchMode());
    }

    @Override
    public boolean isRunning() {
        if (getLoggingHandler() != null && getLoggingHandler().logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED)) {
            getLoggingHandler().flush();
            return true;
        }

        return false;
    }

    @Override
    public boolean isFailed() {
        return getLoggingHandler() != null
                && ERRORS.stream().anyMatch(error -> getLoggingHandler().logsContains(error));
    }

    public boolean isNativeTest() {
        return getLaunchMode() == LaunchMode.NATIVE;
    }

    public void onPreBuild() {

    }

    public void onPostBuild() {

    }

    protected ServiceContext getContext() {
        return serviceContext;
    }

    protected abstract LoggingHandler getLoggingHandler();

    protected LaunchMode getLaunchMode() {
        return launchMode;
    }

    protected boolean needsBuildArtifact() {
        return true;
    }

    protected void validate() {

    }

    private static LaunchMode detectLaunchMode(ServiceContext serviceContext) {
        LaunchMode launchMode = LaunchMode.JVM;
        if (QuarkusProperties.isNativePackageType(serviceContext)) {
            launchMode = LaunchMode.NATIVE;
        } else if (QuarkusProperties.isLegacyJarPackageType(serviceContext)) {
            launchMode = LaunchMode.LEGACY_JAR;
        }

        return launchMode;
    }
}
