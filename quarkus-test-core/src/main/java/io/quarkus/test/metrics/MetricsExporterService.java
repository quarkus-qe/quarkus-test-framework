package io.quarkus.test.metrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.metrics.exporters.FileMetricsExporter;
import io.quarkus.test.metrics.exporters.MetricsExporter;
import io.quarkus.test.metrics.exporters.PrometheusMetricsExporter;
import io.quarkus.test.utils.TestExecutionProperties;

public class MetricsExporterService {

    private static final PropertyLookup METRICS_EXPORT_PROMETHEUS_PROPERTY = new PropertyLookup(
            "metrics.export.prometheus.endpoint");

    private final List<MetricsExporter> exporters = new LinkedList<>();
    private final Map<String, Object> metrics = new HashMap<>();

    public MetricsExporterService() {
        this.exporters.add(new FileMetricsExporter());

        String prometheusHttpEndpoint = METRICS_EXPORT_PROMETHEUS_PROPERTY.get();
        if (StringUtils.isNotEmpty(prometheusHttpEndpoint)) {
            this.exporters.add(new PrometheusMetricsExporter(prometheusHttpEndpoint));
        }
    }

    public <T> T getMetricValue(String metricId, T defaultValue) {
        return (T) metrics.getOrDefault(metricId, defaultValue);
    }

    public void commit(String metricId, Object metricValue) {
        exporters.forEach(exporter -> exporter.commit(metricId, metricValue));
        metrics.put(metricId, metricValue);
    }

    public void push(Map<String, String> labels) {
        for (MetricsExporter exporter : exporters) {
            try {
                exporter.push(TestExecutionProperties.getServiceName(), labels);
            } catch (IOException e) {
                Log.error("Could not push metrics to %s. Caused by %s", exporter.type(), e.getMessage());
            }
        }
    }
}
