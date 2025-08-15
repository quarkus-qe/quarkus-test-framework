package io.quarkus.qe;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class TracesResourceIT {

    private static final String SERVICE_NAME = "grafana-test-app";

    @GrafanaContainer
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication(classes = TracesResource.class, properties = "traces.properties")
    static final RestService app = new RestService()
            .withProperty("quarkus.otel.exporter.otlp.traces.endpoint", grafana::getOtlpCollectorUrl);

    @Test
    public void shouldTrace() {
        String traceId = app.given()
                .get("/traces/Martin")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(Matchers.notNullValue())
                .extract().asString();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> given()
                .get(grafana.getTempoUrl() + "/api/v2/traces/" + traceId)
                .then()
                .body("trace.resourceSpans?.flatten().resource.attributes?.flatten().find { it.key == 'service.name' }.value.stringValue",
                        equalTo(SERVICE_NAME))
                .body("trace.resourceSpans?.flatten().scopeSpans?.flatten().spans?.flatten().attributes?.flatten().find { it.key == 'url.path' }.value.stringValue",
                        equalTo("/traces/Martin"))
                .body("trace.resourceSpans?.flatten().scopeSpans?.flatten().spans?.flatten().attributes?.flatten().find { it.key == 'http.route' }.value.stringValue",
                        equalTo("/traces/{name}")));
    }
}
