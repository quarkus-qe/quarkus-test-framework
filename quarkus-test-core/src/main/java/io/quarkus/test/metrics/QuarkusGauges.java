package io.quarkus.test.metrics;

public class QuarkusGauges {

    private final MetricsExporterService exporter;

    public QuarkusGauges(MetricsExporterService exporter) {
        this.exporter = exporter;
    }

    public void increment(GaugesTypes gaugesTypes) {
        int currentGaugeValue = getCurrentGaugeValue(gaugesTypes.getCode());
        exporter.commit(gaugesTypes.getCode(), currentGaugeValue + 1);
    }

    public void push(QuarkusLabels labels) {
        exporter.push(labels.getLabelsBucket());
    }

    private int getCurrentGaugeValue(String gaugeBucketID) {
        return exporter.getMetricValue(gaugeBucketID, 0);
    }
}
