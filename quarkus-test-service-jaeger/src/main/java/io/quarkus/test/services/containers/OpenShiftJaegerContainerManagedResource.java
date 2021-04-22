package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;
import static java.util.regex.Pattern.quote;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public class OpenShiftJaegerContainerManagedResource implements ManagedResource {

    private static final String JAEGER_IMAGE = "jaegertracing/all-in-one";
    private static final int REST_PORT = 14268;
    private static final int TRACE_PORT = 16686;

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "openshift.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "openshift.template";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/jaeger-deployment-template.yml";
    private static final String DEPLOYMENT = "jaeger.yml";

    private static final String EXPECTED_LOG = "Starting GRPC server";

    private final JaegerContainerManagedResourceBuilder model;
    private final OpenShiftClient client;

    private LoggingHandler loggingHandler;
    private boolean running;

    protected OpenShiftJaegerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
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
        return model.getContext().getOwner().getName() + "-query";
    }

    @Override
    public int getPort(Protocol protocol) {
        return REST_PORT;
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

    private void applyDeployment() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), deploymentFile,
                this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));

        model.getContext().put(JAEGER_TRACE_URL_PROPERTY, getTraceUrl());
    }

    private String replaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getOwner().getName());
        }

        return content.replaceAll(quote("${IMAGE}"), JAEGER_IMAGE)
                .replaceAll(quote("${VERSION}"), getJaegerVersion())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName());
    }

    protected String getTraceUrl() {
        return "http://" + getHost(Protocol.HTTP) + ":" + TRACE_PORT;
    }

    protected String getJaegerVersion() {
        return StringUtils.defaultIfBlank(model.getVersion(), "latest");
    }

}
