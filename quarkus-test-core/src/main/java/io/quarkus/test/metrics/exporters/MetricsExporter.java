package io.quarkus.test.metrics.exporters;

import java.io.IOException;
import java.util.Map;

public interface MetricsExporter {
    String type();

    void commit(String metricId, Object metricValue);

    void push(String serviceName, Map<String, String> labels) throws IOException;
}
