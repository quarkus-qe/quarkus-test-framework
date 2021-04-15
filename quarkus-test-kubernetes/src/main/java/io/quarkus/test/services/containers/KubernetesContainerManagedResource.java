package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;

public class KubernetesContainerManagedResource implements ManagedResource {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "kubernetes.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "kubernetes.template";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/kubernetes-deployment-template.yml";

    private static final String DEPLOYMENT = "kubernetes.yml";

    private final ContainerManagedResourceBuilder model;
    private final KubectlClient client;

    private LoggingHandler loggingHandler;
    private boolean running;

    protected KubernetesContainerManagedResource(ContainerManagedResourceBuilder model) {
        this.model = model;
        this.client = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        applyDeployment();

        client.expose(model.getContext().getOwner(), model.getPort());

        client.scaleTo(model.getContext().getOwner(), 1);
        running = true;

        loggingHandler = new KubernetesLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        client.scaleTo(model.getContext().getOwner(), 0);
        running = false;
    }

    @Override
    public String getHost(Protocol protocol) {
        return client.url(model.getContext().getOwner());
    }

    @Override
    public int getPort(Protocol protocol) {
        return client.port(model.getContext().getOwner());
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
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT);
        client.applyServiceProperties(model.getContext().getOwner(), deploymentFile, this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String replaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getName());
        }

        return content.replaceAll(quote("${IMAGE}"), model.getImage())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + model.getPort());
    }

}
