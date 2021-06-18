package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.logging.Log;

public abstract class TemplateOpenShiftQuarkusApplicationManagedResource<T extends QuarkusApplicationManagedResourceBuilder>
        extends OpenShiftQuarkusApplicationManagedResource<T> {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "openshift.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "openshift.template";
    private static final String DEPLOYMENT = "openshift.yml";

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    public TemplateOpenShiftQuarkusApplicationManagedResource(T model) {
        super(model);
    }

    protected abstract String getDefaultTemplate();

    protected String replaceDeploymentContent(String content) {
        return content;
    }

    @Override
    protected void doInit() {
        applyTemplate();
        awaitForImageStreams();
    }

    @Override
    protected void doUpdate() {
        applyTemplate();
    }

    protected int getInternalPort() {
        String internalPort = model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY);
        if (StringUtils.isNotBlank(internalPort)) {
            return Integer.parseInt(internalPort);
        }

        return INTERNAL_PORT_DEFAULT;
    }

    private void applyTemplate() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                getDefaultTemplate());

        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::internalReplaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String internalReplaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }

        content = content.replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());

        return replaceDeploymentContent(content);
    }

    private void awaitForImageStreams() {
        Log.info(model.getContext().getOwner(), "Waiting for image streams ... ");
        client.awaitFor(model.getContext().getOwner(), model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

}
