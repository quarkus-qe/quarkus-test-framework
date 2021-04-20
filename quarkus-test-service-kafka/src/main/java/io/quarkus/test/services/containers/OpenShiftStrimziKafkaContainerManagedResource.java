package io.quarkus.test.services.containers;

import static java.util.regex.Pattern.quote;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public class OpenShiftStrimziKafkaContainerManagedResource implements ManagedResource {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "openshift.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "openshift.template";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/strimzi-deployment-template.yml";
    private static final String DEPLOYMENT = "kafka.yml";

    private static final String REGISTRY_DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/apicurio-deployment-template.yml";
    private static final String REGISTRY_DEPLOYMENT = "registry.yml";
    private static final int REGISTRY_PORT = 8080;

    private static final String EXPECTED_LOG = "started (kafka.server.KafkaServer)";

    private static final int HTTP_PORT = 9092;

    private final KafkaContainerManagedResourceBuilder model;
    private final OpenShiftClient client;

    private DefaultService registry;
    private LoggingHandler loggingHandler;
    private boolean running;

    protected OpenShiftStrimziKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        applyDeployment();
        client.scaleTo(model.getContext().getOwner(), 1);

        if (model.isWithRegistry()) {
            createRegistryService();
            applyRegistryDeployment();
        }

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
        // Strimzi Kafka only allows to expose Routes using SSL, therefore we'll use internal service routing.
        // TODO: Make it public using the Strimzi Operator:
        // https://developers.redhat.com/blog/2019/06/10/accessing-apache-kafka-in-strimzi-part-3-red-hat-openshift-routes/
        return model.getContext().getOwner().getName();
    }

    @Override
    public int getPort(Protocol protocol) {
        return HTTP_PORT;
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(EXPECTED_LOG);
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

    private void createRegistryService() {
        registry = new DefaultService();
        registry.register("registry");
    }

    private void applyDeployment() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private void applyRegistryDeployment() {
        client.applyServicePropertiesUsingTemplate(registry, REGISTRY_DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT,
                content -> content.replaceAll(quote("${KAFKA_BOOTSTRAP_URL}"), getKafkaBootstrapUrl()),
                model.getContext().getServiceFolder().resolve(REGISTRY_DEPLOYMENT));

        client.expose(registry, REGISTRY_PORT);
        client.scaleTo(registry, 1);

        model.getContext().put(KafkaService.KAFKA_REGISTRY_URL_PROPERTY, getSchemaRegistryUrl());
    }

    private String getSchemaRegistryUrl() {
        return client.url(registry) + "/api";
    }

    private String getKafkaBootstrapUrl() {
        return getHost(Protocol.HTTP).replace("http://", "") + ":" + getPort(Protocol.HTTP);
    }

    private String replaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }

        return content.replaceAll(quote("${IMAGE}"), getKafkaImage())
                .replaceAll(quote("${VERSION}"), getKafkaVersion())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName());
    }

    protected String getKafkaImage() {
        return StringUtils.defaultIfBlank(model.getImage(), model.getVendor().getImage());
    }

    protected String getKafkaVersion() {
        return StringUtils.defaultIfBlank(model.getVersion(), model.getVendor().getDefaultVersion());
    }

}
