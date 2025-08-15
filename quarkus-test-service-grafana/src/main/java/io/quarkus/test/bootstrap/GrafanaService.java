package io.quarkus.test.bootstrap;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_LOKI_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_PROMETHEUS_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_TEMPO_URL_PROPERTY;

public class GrafanaService extends BaseService<GrafanaService> {
    public String getOtlpCollectorUrl() {
        return getPropertyFromContext(GRAFANA_COLLECTOR_URL_PROPERTY.getName()).toString();
    }

    public String getWebUIUrl() {
        return getURI(Protocol.HTTP).toString();
    }

    public String getLokiUrl() {
        return getPropertyFromContext(GRAFANA_LOKI_URL_PROPERTY.getName()).toString();
    }

    public String getTempoUrl() {
        return getPropertyFromContext(GRAFANA_TEMPO_URL_PROPERTY.getName()).toString();
    }

    public String getPrometheusUrl() {
        return getPropertyFromContext(GRAFANA_PROMETHEUS_URL_PROPERTY.getName()).toString();
    }

    /**
     * @deprecated use {@link #getLokiUrl()}
     */
    @Deprecated(forRemoval = true)
    public String getRestUrl() {
        return getLokiUrl();
    }
}
