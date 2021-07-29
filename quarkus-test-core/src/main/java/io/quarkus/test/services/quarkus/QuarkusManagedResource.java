package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.logging.LoggingHandler;

public abstract class QuarkusManagedResource implements ManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String UNRECOVERABLE_ERROR = "Failed to start application";

    @Override
    public boolean isRunning() {
        return getLoggingHandler() != null && getLoggingHandler().logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    @Override
    public boolean isFailed() {
        return getLoggingHandler() != null && getLoggingHandler().logsContains(UNRECOVERABLE_ERROR);
    }

    protected abstract LoggingHandler getLoggingHandler();

    protected boolean needsBuildArtifact() {
        return true;
    }

    protected void validate() {
    }
}
