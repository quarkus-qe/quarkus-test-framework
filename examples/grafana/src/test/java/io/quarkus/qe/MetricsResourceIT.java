package io.quarkus.qe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
public class MetricsResourceIT {

    @GrafanaContainer
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication(classes = MetricsResource.class, properties = "metrics.properties")
    static final RestService app = new RestService()
            .withProperty("quarkus.otel.exporter.otlp.metrics.endpoint", grafana::getOtlpCollectorUrl);

    @Test
    public void shouldRecordMetrics() {
        callMetricsEndpoint();
        callMetricsEndpoint();
        callMetricsEndpoint();
        AwaitilityUtils.untilAsserted(() -> given()
                .queryParam("query", "hello_metrics_invocations_total")
                .get(grafana.getPrometheusUrl() + "/api/v1/query")
                .then().statusCode(200)
                .body("data.result?.flatten().value.flatten().get(1)", is("3")));
    }

    private void callMetricsEndpoint() {
        app.given()
                .get("/metrics")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("hello-metrics"));
    }
}
