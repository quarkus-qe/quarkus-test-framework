package io.quarkus.test.metrics;

public enum HistogramTypes {
    SCENARIO_TEST_TIME_SEC("scenario_duration_seconds");

    private String code;

    HistogramTypes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
