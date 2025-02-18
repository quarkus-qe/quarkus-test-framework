package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_REST_URL_PROPERTY;
import static java.util.regex.Pattern.quote;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;

public class OpenShiftGrafanaContainerManagedResource extends OpenShiftContainerManagedResource {

    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/grafana-deployment-template.yml";
    private static final int GRPC_PORT_NUMBER = 4317;

    private static final String REST_SUFFIX = "-rest";
    private static final String COLLECTOR_SUFFIX = "-collector";
    private static final String WEB_SUFFIX = "-web";

    private final GrafanaContainerManagedResourceBuilder model;

    protected OpenShiftGrafanaContainerManagedResource(GrafanaContainerManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    @Override
    protected String getTemplateByDefault() {
        return DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT;
    }

    @Override
    protected boolean useInternalServiceByDefault() {
        return true;
    }

    @Override
    protected String getInternalServiceName() {
        return model.getContext().getName() + WEB_SUFFIX;
    }

    @Override
    protected void exposeService() {
        super.exposeService();
        // We need to expose an additional endpoint for rest
        String restServiceName = model.getContext().getName() + REST_SUFFIX;
        getClient().expose(restServiceName, model.getRestPort());
        model.getContext().put(GRAFANA_REST_URL_PROPERTY.getName(), getClient().url(restServiceName).toString());

        model.getContext().put(GRAFANA_COLLECTOR_URL_PROPERTY.getName(),
                new URILike(Protocol.HTTP.getValue(), model.getContext().getName() + COLLECTOR_SUFFIX, GRPC_PORT_NUMBER, ""));
    }

    @Override
    protected String replaceDeploymentContent(String content) {
        return super.replaceDeploymentContent(content)
                .replaceAll(quote("${REST_API_PORT}"), "" + model.getRestPort())
                .replaceAll(quote("${WEB_UI_PORT}"), "" + model.getPort())
                .replaceAll(quote("${OTLP_GRPC_PORT}"), "" + model.getOtlpGrpcPort());
    }

}
