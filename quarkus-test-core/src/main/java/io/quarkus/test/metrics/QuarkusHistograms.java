package io.quarkus.test.metrics;

import static io.prometheus.client.Histogram.Timer;
import static io.quarkus.test.metrics.QuarkusLabels.MODULE_STATUS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.PushGateway;

public class QuarkusHistograms {

    private final PushGateway prometheusClient;
    private final Map<String, Histogram> histogramsBucket = new HashMap<>();
    private final CollectorRegistry defaultRegistry;
    private final Map<String, Timer> gaugeTimersBucket = new HashMap<>();
    private final Map<HistogramTypes, CollectorRegistry> modulesRegistry = new HashMap<>();
    private final QuarkusLabels labels;

    public QuarkusHistograms(String prometheusHttpEndpoint) {
        labels = new QuarkusLabels();
        prometheusClient = new PushGateway(prometheusHttpEndpoint);
        modulesRegistry.put(HistogramTypes.MODULE_TEST_TIME_SEC, new CollectorRegistry());
        defaultRegistry = new CollectorRegistry();
    }

    public void startDurationBeforeAll(HistogramTypes histogramTypes) {
        String histogramBucketID = getHistogramBucketID(histogramTypes);
        createHistogramIfNotExist(histogramTypes, histogramBucketID);
        labels.addModuleNameLabel();
        Timer duration = histogramsBucket.get(histogramBucketID).startTimer();
        gaugeTimersBucket.put(histogramBucketID, duration);
    }

    public void stopDurationAfterAll(HistogramTypes histogramTypes) {
        String histogramBucketID = getHistogramBucketID(histogramTypes);
        for (Map.Entry<String, Timer> durations : gaugeTimersBucket.entrySet()) {
            if (durations.getKey().equalsIgnoreCase(histogramBucketID)) {
                Double totalTime = durations.getValue().observeDuration();
            }
        }
    }

    public void push() {
        try {
            for (Map.Entry<HistogramTypes, CollectorRegistry> registry : modulesRegistry.entrySet()) {
                Map<String, String> registryLabels = labels.getLabelsBucket();
                registryLabels.put(MODULE_STATUS, registry.getKey().name().toLowerCase());
                prometheusClient.pushAdd(registry.getValue(), labels.getServiceName(), registryLabels);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getHistogramBucketID(HistogramTypes histogramTypes) {
        return histogramTypes.getCode() + "_" + histogramTypes;
    }

    private void createHistogramIfNotExist(HistogramTypes histogramTypes, String histogramBucketID) {
        if (!histogramsBucket.containsKey(histogramBucketID)) {
            CollectorRegistry registry = Optional.ofNullable(modulesRegistry.get(histogramTypes)).orElse(defaultRegistry);
            Histogram histogramModule = Histogram.build()
                    .name(histogramTypes.getCode()).help("Test latency in seconds.").register(registry);
            histogramsBucket.put(histogramBucketID, histogramModule);
        }
    }
}
