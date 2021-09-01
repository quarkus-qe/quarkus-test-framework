package io.quarkus.test.metrics.exporters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.text.TextStringBuilder;

import io.quarkus.test.configuration.PropertyLookup;

public class FileMetricsExporter implements MetricsExporter {

    private static final PropertyLookup METRICS_EXPORT_FILE_OUTPUT = new PropertyLookup(
            "metrics.export.file.output", "target/logs/metrics.out");

    private final Map<String, Object> metrics = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "file";
    }

    @Override
    public void commit(String metricId, Object metricValue) {
        metrics.put(metricId, metricValue);
    }

    @Override
    public void push(String serviceName, Map<String, String> labels) throws IOException {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.putAll(labels);
        allMetrics.putAll(metrics);

        TextStringBuilder sw = new TextStringBuilder();
        allMetrics.keySet().stream()
                .sorted()
                .forEach(metricId -> sw.appendln(String.format("%s=%s", metricId, allMetrics.get(metricId))));

        Files.write(Path.of(METRICS_EXPORT_FILE_OUTPUT.get()), sw.toString().getBytes());
    }
}
