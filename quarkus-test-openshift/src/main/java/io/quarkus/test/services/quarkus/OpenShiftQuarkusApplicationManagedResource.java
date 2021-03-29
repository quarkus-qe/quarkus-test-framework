package io.quarkus.test.services.quarkus;

import java.util.List;

import org.apache.http.HttpStatus;

import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.inject.OpenShiftFacade;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public abstract class OpenShiftQuarkusApplicationManagedResource implements QuarkusManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final int EXTERNAL_PORT = 80;

    protected final QuarkusApplicationManagedResourceBuilder model;
    protected final OpenShiftFacade facade;

    private LoggingHandler loggingHandler;
    private boolean running;

    public OpenShiftQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
    }

    protected abstract void doStart();

    @Override
    public void start() {
        if (running) {
            return;
        }

        doStart();

        facade.setReplicaTo(model.getContext().getName(), 1);
        running = true;

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        facade.setReplicaTo(model.getContext().getName(), 0);
        running = false;
    }

    @Override
    public String getHost() {
        return facade.getUrlFromRoute(model.getContext().getName());
    }

    @Override
    public int getPort() {
        return EXTERNAL_PORT;
    }

    @Override
    public boolean isRunning() {
        return appIsStarted() && routeIsReachable();
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    private boolean appIsStarted() {
        return loggingHandler != null && loggingHandler.logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    private boolean routeIsReachable() {
        return model.getContext().getOwner().restAssured().get().getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
