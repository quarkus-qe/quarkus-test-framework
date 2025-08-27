package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.KUBERNETES_DEPLOYMENT_SERVICE_PROPERTY;
import static java.util.regex.Pattern.quote;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.services.URILike;

public class KubernetesContainerManagedResource implements ManagedResource {

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
    public String getDisplayName() {
        return model.getImage();
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
    public URILike getURI(Protocol protocol) {
        if (useInternalServiceAsUrl()) {
            return createURI(protocol.getValue(), model.getContext().getName(), model.getPort());
        }

        return createURI("http",
                client.host(),
                client.port(model.getContext().getOwner()));
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(model.getExpectedLog());
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    protected String getTemplate() {
        return getConfiguration()
                .getOrDefault(Configuration.Property.KUBERNETES_DEPLOYMENT_TEMPLATE_PROPERTY,
                        DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT);
    }

    private void applyDeployment() {
        String deploymentFile = getTemplate();
        client.applyServiceProperties(model.getContext().getOwner(), deploymentFile, this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String replaceDeploymentContent(String content) {
        String customServiceName = getConfiguration().get(KUBERNETES_DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getName());
        }
        String args = Arrays.stream(model.getCommand()).map(cmd -> "\"" + cmd + "\"").collect(Collectors.joining(", "));
        return content.replaceAll(quote("${IMAGE}"), model.getImage())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + model.getPort())
                .replaceAll(quote("${ARGS}"), args);
    }

    protected boolean useInternalServiceAsUrl() {
        return Boolean.TRUE.toString()
                .equals(getConfiguration()
                        .get(Configuration.Property.KUBERNETES_USE_INTERNAL_SERVICE_AS_URL_PROPERTY));
    }

    private Configuration getConfiguration() {
        return model.getContext().getOwner().getConfiguration();
    }

    protected KubectlClient getClient() {
        return client;
    }

    protected ContainerManagedResourceBuilder getModel() {
        return model;
    }
}
