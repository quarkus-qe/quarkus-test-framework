package io.quarkus.test.services.quarkus;

import static io.quarkus.test.bootstrap.inject.OpenShiftClient.TLS_ROUTE_SUFFIX;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.EXTERNAL_SSL_PORT;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getInternalHttpsPort;
import static io.quarkus.test.security.certificate.ServingCertificateConfig.isServingCertificateScenario;
import static io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationCertificateConfigurator.KEYSTORE_MOUNT_PATH;
import static io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationCertificateConfigurator.PROPERTY_KEYSTORE_SECRET_NAME;
import static io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationCertificateConfigurator.PROPERTY_TRUSTSTORE_SECRET_NAME;
import static io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationCertificateConfigurator.TRUSTSTORE_MOUNT_PATH;
import static java.util.regex.Pattern.quote;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.security.certificate.CertificateBuilder;

public abstract class TemplateOpenShiftQuarkusApplicationManagedResource<T extends QuarkusApplicationManagedResourceBuilder>
        extends OpenShiftQuarkusApplicationManagedResource<T> {

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

    protected final String getTemplate() {
        return model.getContext().getOwner().getConfiguration()
                .getOrDefault(Configuration.Property.OPENSHIFT_DEPLOYMENT_TEMPLATE_PROPERTY, getDefaultTemplate());
    }

    private void applyTemplate() {
        String deploymentFile = getTemplate();
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::internalReplaceDeploymentContent,
                addExtraTemplateProperties(),
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));

        if (model.isSslEnabled()) {
            client.exposeDeploymentPort(model.getContext().getName(), "https", model.getOcpTlsPort());
            client.createService(model.getContext().getName(),
                    model.getContext().getName() + TLS_ROUTE_SUFFIX, model.getOcpTlsPort());
            client.createTlsPassthroughRoute(model.getContext().getName() + TLS_ROUTE_SUFFIX,
                    model.getContext().getName() + TLS_ROUTE_SUFFIX,
                    model.getOcpTlsPort());
        }

        // if certificate builder is set, there should be secrets set
        // properties are set to context by OpenShiftQuarkusApplicationCertificateConfigurator
        CertificateBuilder certificateBuilder = model.getContext().get(CertificateBuilder.INSTANCE_KEY);
        if (certificateBuilder != null && !certificateBuilder.certificates().isEmpty()) {
            String appName = model.getContext().getName();
            client.mountSecretToDeployment(appName, model.getContext().get(PROPERTY_KEYSTORE_SECRET_NAME),
                    KEYSTORE_MOUNT_PATH);
            client.mountSecretToDeployment(appName, model.getContext().get(PROPERTY_TRUSTSTORE_SECRET_NAME),
                    TRUSTSTORE_MOUNT_PATH);
        }
    }

    private String internalReplaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner()
                .getConfiguration()
                .get(Configuration.Property.OPENSHIFT_DEPLOYMENT_SERVICE_PROPERTY);
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
