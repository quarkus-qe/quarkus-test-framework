package io.quarkus.qe;

import static io.quarkus.qe.resources.PrometheusPushGatewayContainer.REST_PORT;
import static io.quarkus.test.metrics.MetricsExtensionBootstrap.METRIC_FORCE_PUSH_TAG;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.qe.resources.PrometheusPushGatewayContainer;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MetricsExtensionBootstrapIT {
    static final String REQUEST_SUCCESS = "data.ts_quarkus_gauge_requests_success.metrics.value";
    static final String REQUEST_TOTAL = "data.ts_quarkus_gauge_requests_total.metrics.value";

    static PrometheusPushGatewayContainer prometheusPushGatewayContainer = new PrometheusPushGatewayContainer();

    private List<List<String>> metricsResult = new ArrayList<>();

    @BeforeAll
    static public void tearUp() {
        prometheusPushGatewayContainer.start();
    }

    @AfterAll
    static public void tearDown() {
        prometheusPushGatewayContainer.stop();
    }

    @Test
    @Tag(METRIC_FORCE_PUSH_TAG)
    @Order(1)
    public void successExample() {
        Assertions.assertTrue(true);
    }

    @Test
    @Order(2)
    public void metricsMustBeAvailable() {
        whenRetrieveMetrics(REQUEST_SUCCESS);
        thenCheckMetricsIsNotEmpty(REQUEST_SUCCESS);
        thenCheckMetricsGreaterThan(REQUEST_SUCCESS, 0);

        whenRetrieveMetrics(REQUEST_TOTAL);
        thenCheckMetricsIsNotEmpty(REQUEST_TOTAL);
        thenCheckMetricsGreaterThan(REQUEST_TOTAL, 0);
    }

    private void whenRetrieveMetrics(String metricId) {
        metricsResult = RestAssured.given().baseUri("http://localhost")
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

    private void thenCheckMetricsIsNotEmpty(String metricId) {
        Assertions.assertFalse(metricsResult.isEmpty(), "metric " + metricId + " should not be empty");
    }

    private void thenCheckMetricsGreaterThan(String metricId, int threshold) {
        for (List<String> metrics : metricsResult) {
            int testsAmount = Integer.parseInt(metrics.get(0));
            Assertions.assertTrue(testsAmount > threshold, "Unexpected " + metricId);
        }
    }

}
