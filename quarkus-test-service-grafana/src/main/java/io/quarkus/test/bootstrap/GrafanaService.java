package io.quarkus.test.bootstrap;

import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_COLLECTOR_URL_PROPERTY;
import static io.quarkus.test.configuration.Configuration.Property.GRAFANA_REST_URL_PROPERTY;

public class GrafanaService extends BaseService<GrafanaService> {
    public String getOtlpCollectorUrl() {
        return getPropertyFromContext(GRAFANA_COLLECTOR_URL_PROPERTY.getName()).toString();
    }

    public String getWebUIUrl() {
        return getURI(Protocol.HTTP).toString();
    }

    public String getRestUrl() {
        return getPropertyFromContext(GRAFANA_REST_URL_PROPERTY.getName()).toString();
    }
}
