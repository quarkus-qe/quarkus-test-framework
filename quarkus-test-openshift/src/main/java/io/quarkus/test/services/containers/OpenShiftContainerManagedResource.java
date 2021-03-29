package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.inject.OpenShiftFacade;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftContainerManagedResource implements ManagedResource {

    private static final String DEPLOYMENT_TEMPLATE = "/openshift-deployment-template.yml";

    private static final int HTTP_PORT = 80;

    private final ContainerManagedResourceBuilder model;
    private final OpenShiftFacade facade;

    private LoggingHandler loggingHandler;
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

        applyDeployment();
        facade.exposeService(model.getContext().getName(), model.getPort());

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

    private void applyDeployment() {
        String deploymentContent = FileUtils.loadFile(DEPLOYMENT_TEMPLATE)
                .replaceAll(quote("${NAMESPACE}"), facade.getNamespace())
                .replaceAll(quote("${IMAGE}"), model.getImage())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + model.getPort());

        deploymentContent = facade.addPropertiesToDeployment(model.getContext().getOwner().getProperties(), deploymentContent);

        facade.apply(FileUtils.copyContentTo(model.getContext(), deploymentContent,
                model.getContext().getName() + "-deployment.yml"));
    }

}
