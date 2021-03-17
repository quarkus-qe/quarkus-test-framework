package io.quarkus.test.containers;

import java.util.List;

import io.quarkus.test.ManagedResource;
import io.quarkus.test.extension.OpenShiftExtensionBootstrap;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;
import io.quarkus.test.openshift.OpenShiftFacade;

public class OpenShiftContainerManagedResource implements ManagedResource {

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
            facade.createApplication(model.getContext().getOwner().getName(), model.getImage());
            facade.exposeService(model.getContext().getOwner().getName(), model.getPort());
			init = true;
		}

        facade.setReplicaTo(model.getContext().getOwner().getName(), 1);
		running = true;

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
        loggingHandler.startWatching();
	}

	@Override
	public void stop() {
        facade.setReplicaTo(model.getContext().getOwner().getName(), 0);
		running = false;
	}

	@Override
	public String getHost() {
        return facade.getUrlFromRoute(model.getContext().getOwner().getName());
	}

	@Override
	public int getPort() {
		return 80;
	}

	@Override
	public boolean isRunning() {
        return loggingHandler.logsContains(model.getExpectedLog());
	}

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

}
