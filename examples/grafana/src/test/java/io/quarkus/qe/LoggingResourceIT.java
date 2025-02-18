package io.quarkus.qe;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class LoggingResourceIT {
    private static final String SERVICE_NAME = "grafana-test-app";
    // log line produced by the app
    private static final String TESTING_LOG_LINE = "This is logging test message";

    @GrafanaContainer()
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.otel.exporter.otlp.logs.endpoint", grafana::getOtlpCollectorUrl);

    @Test
    public void shouldLog() {
        app.given()
                .get("logging")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("I'm logging"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = given().when()
                    .queryParam("query", "{service_name=\"" + SERVICE_NAME + "\"}")
                    .get(grafana.getRestUrl() + "/loki/api/v1/query_range")
                    .asString();

            assertTrue(response.contains(TESTING_LOG_LINE), "Server log should contain logged message");
        });
    }
}
