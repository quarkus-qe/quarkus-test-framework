package io.quarkus.test.services.quarkus;

import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.EXTERNAL_SSL_PORT;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getInternalHttpsPort;
import static io.quarkus.test.security.certificate.ServingCertificateConfig.isServingCertificateScenario;
import static java.util.regex.Pattern.quote;

import java.util.Collections;
import java.util.Map;

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
        if (isHttpsScenario()) {
            return getInternalHttpsPort(model.getContext());
        }
        return model.getContext().getOwner().getProperty(QUARKUS_HTTP_PORT_PROPERTY)
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .orElse(INTERNAL_PORT_DEFAULT);
    }

    private boolean isHttpsScenario() {
        return model.isSslEnabled() && isServingCertificateScenario(model.getContext());
    }

    protected Map<String, String> addExtraTemplateProperties() {
        return Collections.emptyMap();
    }

    private void applyTemplate() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                getDefaultTemplate());

        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::internalReplaceDeploymentContent,
                addExtraTemplateProperties(),
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String internalReplaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }

        var ingressInternalPort = (isHttpsScenario() ? EXTERNAL_SSL_PORT : getInternalPort());
        content = content.replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort())
                .replaceAll(quote("${INTERNAL_INGRESS_PORT}"), "" + ingressInternalPort)
                .replace("${MANAGEMENT_PORT}", "" + model.getManagementPort());

        return replaceDeploymentContent(content);
    }

    private void awaitForImageStreams() {
        Log.info(model.getContext().getOwner(), "Waiting for image streams ... ");
        client.awaitFor(model.getContext().getOwner(), model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

}
