package io.quarkus.test.metrics.exporters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.PushGateway;

public class PrometheusMetricsExporter implements MetricsExporter {

    private final PushGateway prometheusClient;
    private final CollectorRegistry defaultRegistry;
    private final Map<String, Gauge> registeredGaugeMetrics = new HashMap<>();
    private final Map<String, Histogram> registeredHistogramMetrics = new HashMap<>();

    public PrometheusMetricsExporter(String prometheusHttpEndpoint) {
        prometheusClient = new PushGateway(prometheusHttpEndpoint);
        defaultRegistry = new CollectorRegistry();
    }

    @Override
    public String type() {
        return "prometheus";
    }

    @Override
    public void commit(String metricId, Object metricValue) {
        if (metricValue instanceof Integer) {
            commitGauge(metricId, (int) metricValue);
        } else if (metricValue instanceof Long) {
            commitHistogram(metricId, (long) metricValue);
        }
    }

    @Override
    public void push(String serviceName, Map<String, String> labels) throws IOException {
        prometheusClient.pushAdd(defaultRegistry, serviceName, labels);
    }

    private void commitGauge(String metricId, int metricValue) {
        Gauge gauge = registeredGaugeMetrics.get(metricId);
        if (gauge == null) {
            gauge = Gauge.build().name(metricId).help("Module gauge").register(defaultRegistry);
            registeredGaugeMetrics.put(metricId, gauge);
        }

        gauge.set(metricValue);
    }

    private void commitHistogram(String metricId, long metricValue) {
        Histogram histogram = registeredHistogramMetrics.get(metricId);
        if (histogram == null) {
            histogram = Histogram.build().name(metricId).help("Test latency in seconds.").register(defaultRegistry);
            registeredHistogramMetrics.put(metricId, histogram);
        }

        histogram.observe(metricValue);
    }
}
