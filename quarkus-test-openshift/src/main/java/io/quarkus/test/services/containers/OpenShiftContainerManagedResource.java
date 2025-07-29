package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.bootstrap.inject.OpenShiftUtils;
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftContainerManagedResource implements ManagedResource {

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

        client.scaleTo(model.getContext().getOwner(), 1);
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
    public URILike getURI(Protocol protocol) {
        if (useInternalServiceAsUrl()) {
            return createURI("http", getInternalServiceName(), model.getPort());
        }
        return client.url(model.getContext().getOwner()).withPort(HTTP_PORT);
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
            if (model.isSslEnabled()) {
                client.exposeDeploymentPort(model.getContext().getName(), "https", model.getTlsPort());
                client.createService(model.getContext().getName(),
                        model.getContext().getName() + "-secured", model.getTlsPort());
                client.createTlsPassthroughRoute(model.getContext().getOwner().getName() + "-secured",
                        model.getContext().getOwner().getName() + "-secured", model.getTlsPort());
            }
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

    private Configuration getConfiguration() {
        return model.getContext().getOwner().getConfiguration();
    }

    protected String replaceDeploymentContent(String content) {
        String customServiceName = getConfiguration()
                .get(Configuration.Property.OPENSHIFT_DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }
        String args = Arrays.stream(model.getCommand()).map(cmd -> "\"" + cmd + "\"").collect(Collectors.joining(", "));
        return content.replaceAll(quote("${IMAGE}"), model.getImage())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + model.getPort())
                .replaceAll(quote("${INTERNAL_INGRESS_PORT}"), "" + model.getPort())
                .replaceAll(quote("${ARGS}"), args)
                .replaceAll(quote("${CURRENT_NAMESPACE}"), client.project());
    }

    private void applyDeployment() {
        String deploymentFile = getConfiguration().getOrDefault(Configuration.Property.OPENSHIFT_DEPLOYMENT_TEMPLATE_PROPERTY,
                getTemplateByDefault());
        Path templateFile = model.getContext().getServiceFolder().resolve(DEPLOYMENT);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(),
                deploymentFile,
                this::replaceDeploymentContent,
                templateFile);
        List<HasMetadata> metadata = client.loadYamlFromFile(templateFile);
        Deployment deployment = OpenShiftUtils.getDeployment(metadata).orElseThrow();
        for (ContainerManagedResourceBuilder.MountConfig mount : model.getMounts()) {
            client.addMount(deployment, mount.from, mount.to);
        }
        String yaml = OpenShiftUtils.toYaml(metadata);
        client.apply(FileUtils.copyContentTo(yaml, templateFile));
    }

    protected boolean useInternalServiceAsUrl() {
        return Boolean.TRUE.toString()
                .equals(getConfiguration()
                        .getOrDefault(Configuration.Property.OPENSHIFT_USE_INTERNAL_SERVICE_AS_URL_PROPERTY,
                                "" + useInternalServiceByDefault()));
    }

}
