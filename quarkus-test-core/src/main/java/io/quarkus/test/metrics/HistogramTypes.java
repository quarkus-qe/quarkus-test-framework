package io.quarkus.test.metrics;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum HistogramTypes {
    MODULE_TEST_TIME_SEC("ts_quarkus_histogram_modules_duration"),
    UNKNOWN("ts_quarkus_histogram_unknown");

    private static final Map<String, HistogramTypes> LOOK_UP = new HashMap<>();
    private String type;

    static {
        for (HistogramTypes histogramTypes : EnumSet.allOf(HistogramTypes.class)) {
            LOOK_UP.put(histogramTypes.getCode(), histogramTypes);
        }
    }

    HistogramTypes(String type) {
        this.type = type;
    }

    public String getCode() {
        return (null != type) ? type : "ts_quarkus_histogram";
    }

    public HistogramTypes getType(final String code) {
        return (LOOK_UP.containsKey(code)) ? LOOK_UP.get(code) : UNKNOWN;
    }
}
