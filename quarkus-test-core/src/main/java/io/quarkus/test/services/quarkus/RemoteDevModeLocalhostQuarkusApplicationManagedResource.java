package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON;
import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.QUARKUS_LAUNCH_DEV_MODE;
import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.QUARKUS_LIVE_RELOAD_PASSWORD;
import static io.quarkus.test.utils.MavenUtils.withProperty;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.test.logging.Log;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.utils.ProcessUtils;

public class RemoteDevModeLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private static final String JAVA = "java";

    private final RemoteDevModeQuarkusApplicationManagedResourceBuilder model;

    private Process remoteDevProcess;

    public RemoteDevModeLocalhostQuarkusApplicationManagedResource(
            RemoteDevModeQuarkusApplicationManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    @Override
    public boolean isRunning() {
        if (super.isRunning()) {
            // We need to wait for the remote dev daemon to be started
            if (remoteDevProcess == null) {
                startRemoteDevProcess();
            }

            if (getLoggingHandler().logsContains(EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON)) {
                getLoggingHandler().flush();
                return true;
            }
        }

        return false;
    }

    @Override
    public void stop() {
        super.stop();
        ProcessUtils.destroy(remoteDevProcess);
    }

    @Override
    protected LaunchMode getLaunchMode() {
        return LaunchMode.REMOTE_DEV;
    }

    protected List<String> prepareCommand(List<String> systemProperties) {
        List<String> command = new LinkedList<>();
        command.add(JAVA);
        command.add(withProperty(QUARKUS_LIVE_RELOAD_PASSWORD, model.getLiveReloadPassword()));
        command.addAll(systemProperties);
        command.add("-jar");
        command.add(model.getArtifact().toAbsolutePath().toString());
        return command;
    }

    @Override
    protected void onPreStart(ProcessBuilder pb) {
        super.onPreStart(pb);

        pb.environment().put(QUARKUS_LAUNCH_DEV_MODE, Boolean.TRUE.toString());
    }

    private synchronized void startRemoteDevProcess() {
        if (remoteDevProcess == null) {
            ProcessBuilder pb = model.prepareRemoteDevProcess();
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(getLogOutputFile()));

            try {
                remoteDevProcess = pb.start();
            } catch (IOException e) {
                Log.error(getContext().getOwner(), "Failed to start the remote dev process. Caused by " + e.getMessage());
            }
        }
    }
}
