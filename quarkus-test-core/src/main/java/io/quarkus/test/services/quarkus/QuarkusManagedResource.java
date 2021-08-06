package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public abstract class QuarkusManagedResource implements ManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String UNRECOVERABLE_ERROR = "Failed to start application";

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
        return getLoggingHandler() != null && getLoggingHandler().logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    @Override
    public boolean isFailed() {
        return getLoggingHandler() != null && getLoggingHandler().logsContains(UNRECOVERABLE_ERROR);
    }

    public boolean isNativeTest() {
        return getLaunchMode() == LaunchMode.NATIVE;
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
