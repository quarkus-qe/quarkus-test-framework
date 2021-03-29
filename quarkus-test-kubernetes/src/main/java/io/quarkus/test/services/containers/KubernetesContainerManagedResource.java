package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.util.List;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.inject.KubectlFacade;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.FileUtils;

public class KubernetesContainerManagedResource implements ManagedResource {

    private static final String DEPLOYMENT_TEMPLATE = "/kubernetes-deployment-template.yml";

    private final ContainerManagedResourceBuilder model;
    private final KubectlFacade facade;

    private LoggingHandler loggingHandler;
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

        applyDeployment();

        facade.exposeService(model.getContext().getName(), model.getPort());

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
