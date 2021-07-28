package io.quarkus.qe;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;

@QuarkusScenario
public class MetricsExtensionBootstrapIT extends MetricsExtensionCommonIT {

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
