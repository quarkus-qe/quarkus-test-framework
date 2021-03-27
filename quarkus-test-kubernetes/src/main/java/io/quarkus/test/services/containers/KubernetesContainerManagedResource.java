package io.quarkus.test.services.containers;

import java.util.List;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.inject.KubectlFacade;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;

public class KubernetesContainerManagedResource implements ManagedResource {

    private final ContainerManagedResourceBuilder model;
    private final KubectlFacade facade;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    protected KubernetesContainerManagedResource(ContainerManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
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

        loggingHandler = new KubernetesLoggingHandler(model.getContext());
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
        return facade.getUrlByService(model.getContext().getName());
    }

    @Override
    public int getPort() {
        return facade.getPortByService(model.getContext().getName());
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
