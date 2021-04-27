package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.logging.LoggingHandler;

public abstract class QuarkusManagedResource implements ManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String UNRECOVERABLE_ERROR = "Failed to start application";

    @Override
    public boolean isRunning() {
        if (getLoggingHandler() == null) {
            return false;
        }

        if (getLoggingHandler().logsContains(UNRECOVERABLE_ERROR)) {
            fail("Application failed to start");
        }

        return getLoggingHandler().logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    protected abstract LoggingHandler getLoggingHandler();

    protected boolean needsBuildArtifact() {
        return true;
    }

    protected void validate() {
    }
}
