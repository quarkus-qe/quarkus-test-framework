package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public class OpenShiftContainerManagedResource implements ManagedResource {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "openshift.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "openshift.template";
    private static final String USE_INTERNAL_SERVICE_AS_URL_PROPERTY = "openshift.use-internal-service-as-url";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/openshift-deployment-template.yml";
    private static final String DEPLOYMENT = "openshift.yml";

    private static final int HTTP_PORT = 80;

    private final ContainerManagedResourceBuilder model;
    private final OpenShiftClient client;

    private LoggingHandler loggingHandler;
    private boolean running;

    protected OpenShiftContainerManagedResource(ContainerManagedResourceBuilder model) {
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
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
        exposeService();

        client.scaleToWhenDcReady(model.getContext().getOwner(), 1);
        running = true;

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
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
        if (useInternalServiceAsUrl()) {
            return protocol.getValue() + "://" + getInternalServiceName();
        }

        return client.url(model.getContext().getOwner());
    }

    @Override
    public int getPort(Protocol protocol) {
        if (useInternalServiceAsUrl()) {
            return model.getPort();
        }

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

    @Override
    public void restart() {
        stop();
        start();
    }

    protected void exposeService() {
        if (!useInternalServiceAsUrl()) {
            client.expose(model.getContext().getOwner(), model.getPort());
        }
    }

    protected String getTemplateByDefault() {
        return DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT;
    }

    protected boolean useInternalServiceByDefault() {
        return false;
    }

    protected String getInternalServiceName() {
        return model.getContext().getName();
    }

    protected OpenShiftClient getClient() {
        return client;
    }

    protected String replaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }

        return content.replaceAll(quote("${IMAGE}"), model.getImage())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + model.getPort())
                .replaceAll(quote("${ARGS}"), String.join(" ", model.getCommand()));
    }

    private void applyDeployment() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                getTemplateByDefault());
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private boolean useInternalServiceAsUrl() {
        return Boolean.TRUE.toString()
                .equals(model.getContext().getOwner().getConfiguration()
                        .getOrDefault(USE_INTERNAL_SERVICE_AS_URL_PROPERTY, "" + useInternalServiceByDefault()));
    }

}
