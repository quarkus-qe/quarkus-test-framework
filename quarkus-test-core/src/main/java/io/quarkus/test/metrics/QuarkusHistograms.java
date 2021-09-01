package io.quarkus.test.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

public class QuarkusHistograms {

    private static final List<HistogramTypes> TYPES = Arrays.asList(HistogramTypes.SCENARIO_TEST_TIME_SEC);

    private final MetricsExporterService exporter;
    private final Map<HistogramTypes, StopWatch> timersBucket = new HashMap<>();

    public QuarkusHistograms(MetricsExporterService exporter) {
        this.exporter = exporter;
    }

    public void startDurationBeforeAll(HistogramTypes histogramTypes) {
        StopWatch timer = new StopWatch();
        timersBucket.put(histogramTypes, timer);
        timer.start();
    }

    public void stopDurationAfterAll(HistogramTypes histogramTypes) {
        StopWatch timer = timersBucket.get(histogramTypes);
        if (timer != null && timer.isStarted()) {
            timer.stop();
            exporter.commit(histogramTypes.getCode(), timer.getTime(TimeUnit.SECONDS));
        }
    }

    public void push(QuarkusLabels labels) {
        exporter.push(labels.getLabelsBucket());
    }
}
