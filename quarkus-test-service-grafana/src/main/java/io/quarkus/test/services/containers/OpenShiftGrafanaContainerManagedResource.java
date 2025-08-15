package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_LOKI_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_PROMETHEUS_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_TEMPO_URL_PROPERTY;
import static java.util.regex.Pattern.quote;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;

public class OpenShiftGrafanaContainerManagedResource extends OpenShiftContainerManagedResource {

    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/grafana-deployment-template.yml";
    private static final int GRPC_PORT_NUMBER = 4317;

    private static final String LOKI_SUFFIX = "-loki";
    private static final String TEMPO_SUFFIX = "-tempo";
    private static final String PROMETHEUS_SUFFIX = "-prometheus";
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
        // We need to expose an additional endpoint for Loki
        String lokiServiceName = model.getContext().getName() + LOKI_SUFFIX;
        getClient().expose(lokiServiceName, model.getLokiPort());
        model.getContext().put(GRAFANA_LOKI_URL_PROPERTY.getName(), getClient().url(lokiServiceName).toString());
        // We need to expose an additional endpoint for Tempo
        String tempoServiceName = model.getContext().getName() + TEMPO_SUFFIX;
        getClient().expose(tempoServiceName, model.getTempoPort());
        model.getContext().put(GRAFANA_TEMPO_URL_PROPERTY.getName(), getClient().url(tempoServiceName).toString());
        // We need to expose an additional endpoint for Prometheus
        String prometheusServiceName = model.getContext().getName() + PROMETHEUS_SUFFIX;
        getClient().expose(prometheusServiceName, model.getPrometheusPort());
        model.getContext().put(GRAFANA_PROMETHEUS_URL_PROPERTY.getName(), getClient().url(prometheusServiceName).toString());

        model.getContext().put(GRAFANA_COLLECTOR_URL_PROPERTY.getName(),
                new URILike(Protocol.HTTP.getValue(), model.getContext().getName() + COLLECTOR_SUFFIX, GRPC_PORT_NUMBER, ""));
    }

    @Override
    protected String replaceDeploymentContent(String content) {
        return super.replaceDeploymentContent(content)
                .replaceAll(quote("${LOKI_API_PORT}"), "" + model.getLokiPort())
                .replaceAll(quote("${TEMPO_API_PORT}"), "" + model.getTempoPort())
                .replaceAll(quote("${PROMETHEUS_API_PORT}"), "" + model.getPrometheusPort())
                .replaceAll(quote("${WEB_UI_PORT}"), "" + model.getPort())
                .replaceAll(quote("${OTLP_GRPC_PORT}"), "" + model.getOtlpGrpcPort());
    }

}
