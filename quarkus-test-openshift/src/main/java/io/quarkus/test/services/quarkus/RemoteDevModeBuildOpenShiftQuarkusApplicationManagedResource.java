package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON;
import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.QUARKUS_LAUNCH_DEV_MODE;
import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.QUARKUS_LIVE_RELOAD_PASSWORD;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.logging.FileServiceLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.ProcessUtils;

public class RemoteDevModeBuildOpenShiftQuarkusApplicationManagedResource
        extends BuildOpenShiftQuarkusApplicationManagedResource {

    private static final String REMOTE_DEV_LOG_OUTPUT_FILE = "remote-dev-out.log";

    private final RemoteDevModeQuarkusApplicationManagedResourceBuilder model;

    private Process remoteDevProcess;
    private File remoteDevLogFile;
    private LoggingHandler remoteDevLoggingHandler;

    public RemoteDevModeBuildOpenShiftQuarkusApplicationManagedResource(
            RemoteDevModeQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
        this.remoteDevLogFile = new File(model.getContext().getServiceFolder().resolve(REMOTE_DEV_LOG_OUTPUT_FILE).toString());
    }

    @Override
    public boolean isRunning() {
        if (super.isRunning()) {
            // We need to wait for the remote dev daemon to be started
            if (remoteDevProcess == null) {
                startRemoteDevProcess();
            }

            if (remoteDevLoggingHandler != null
                    && remoteDevLoggingHandler.logsContains(EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON)) {
                remoteDevLoggingHandler.flush();
                return true;
            }
        }

        return false;
    }

    @Override
    public void stop() {
        super.stop();
        if (remoteDevLoggingHandler != null) {
            remoteDevLoggingHandler.stopWatching();
        }

        ProcessUtils.destroy(remoteDevProcess);
    }

    @Override
    protected Map<String, String> addExtraTemplateProperties() {
        Map<String, String> templateProperties = new HashMap<>();
        templateProperties.putAll(super.addExtraTemplateProperties());
        templateProperties.put(QUARKUS_LAUNCH_DEV_MODE, Boolean.TRUE.toString());
        templateProperties.put(QUARKUS_LIVE_RELOAD_PASSWORD, model.getLiveReloadPassword());

        return templateProperties;
    }

    private synchronized void startRemoteDevProcess() {
        if (remoteDevProcess == null) {
            ProcessBuilder pb = model.prepareRemoteDevProcess();
            pb.redirectOutput(remoteDevLogFile);

            try {
                remoteDevProcess = pb.start();

                remoteDevLoggingHandler = new FileServiceLoggingHandler(model.getContext().getOwner(), remoteDevLogFile);
                remoteDevLoggingHandler.startWatching();
            } catch (IOException e) {
                Log.error(getContext().getOwner(), "Failed to start the remote dev process. Caused by " + e.getMessage());
            }
        }
    }
}
