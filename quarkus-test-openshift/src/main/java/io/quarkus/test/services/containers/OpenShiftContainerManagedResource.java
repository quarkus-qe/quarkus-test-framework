package io.quarkus.test.services.containers;

import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.inject.OpenShiftFacade;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public class OpenShiftContainerManagedResource implements ManagedResource {

    private static final int HTTP_PORT = 80;

    private final ContainerManagedResourceBuilder model;
    private final OpenShiftFacade facade;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    protected OpenShiftContainerManagedResource(ContainerManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!init) {
            facade.createApplication(model.getContext().getName(), model.getImage());
            facade.exposeService(model.getContext().getName(), model.getPort());
            init = true;
        }

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
        return HTTP_PORT;
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(model.getExpectedLog());
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

}
