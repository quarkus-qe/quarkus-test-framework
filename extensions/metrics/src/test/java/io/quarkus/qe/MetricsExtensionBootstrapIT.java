package io.quarkus.qe;

import static io.quarkus.qe.resources.PrometheusPushGatewayContainer.REST_PORT;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.qe.resources.PrometheusPushGatewayContainer;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.utils.AwaitilityUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class MetricsExtensionBootstrapIT {

    static final String TESTS_SUCCEED = "data.tests_succeed.metrics.value";
    static final String TESTS_IGNORED = "data.tests_ignored.metrics.value";
    static final String TESTS_TOTAL = "data.tests_total.metrics.value";
    static final Path METRICS_FILE_IN_TARGET = Path.of("target", "logs", "metrics.out");

    @Container
    static PrometheusPushGatewayContainer prometheusPushGatewayContainer = new PrometheusPushGatewayContainer();

    @Test
    @Order(1)
    public void successExample() {
        assertTrue(true);
    }

    @Test
    @Order(2)
    @Disabled("to verify disabled metrics")
    public void disabledExample() {
        // ignored
    }

    @Test
    @Order(3)
    public void metricsMustBeAvailable() {
        thenMetricIs(TESTS_SUCCEED, 1);
        thenMetricIs(TESTS_IGNORED, 1);
        thenMetricIs(TESTS_TOTAL, 2);
    }

    @Test
    @Order(4)
    public void metricsShouldBeExportedToFile() throws IOException {
        String metricsInFile = Files.readString(METRICS_FILE_IN_TARGET);
        assertTrue(metricsInFile.contains("tests_succeed=2"), "Unexpected metrics content: " + metricsInFile);
    }

    protected List<List<String>> getMetricsById(String metricId) {
        return RestAssured.given().baseUri("http://localhost")
                .port(REST_PORT)
                .contentType(ContentType.JSON)
                .basePath("/api/v1")
                .when()
                .get("/metrics")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("status", is("success"))
                .extract().jsonPath().getList(metricId);
    }

    private void thenMetricIs(String metricId, int expectedValue) {
        AwaitilityUtils.untilAsserted(() -> {
            List<List<String>> metricsResult = getMetricsById(metricId);
            assertFalse(metricsResult.isEmpty(), "metric " + metricId + " should not be empty. Metrics were: " + metricsResult);

            for (List<String> metrics : metricsResult) {
                int actual = Integer.parseInt(metrics.get(0));
                assertEquals(expectedValue, actual, "Unexpected value for " + metricId);
            }
        });

    }
}
