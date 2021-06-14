package io.quarkus.test.metrics;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum GaugesTypes {
    TOTAL("ts_quarkus_gauge_requests_total"),
    TOTAL_SUCCESS("ts_quarkus_gauge_requests_success"),
    TOTAL_IGNORE("ts_quarkus_gauge_requests_ignore"),
    TOTAL_FAIL("ts_quarkus_gauge_requests_fail"),
    MODULE_SUCCESS("ts_quarkus_gauge_modules"),
    MODULE_IGNORE("ts_quarkus_gauge_modules"),
    MODULE_FAIL("ts_quarkus_gauge_modules"),
    UNKNOWN("ts_quarkus_gauge_unknown");

    private static final Map<String, GaugesTypes> LOOK_UP = new HashMap<>();
    private String type;

    static {
        for (GaugesTypes gaugesTypes : EnumSet.allOf(GaugesTypes.class)) {
            LOOK_UP.put(gaugesTypes.getCode(), gaugesTypes);
        }
    }

    GaugesTypes(String type) {
        this.type = type;
    }

    public String getCode() {
        return (null != type) ? type : "ts_quarkus_gauge";
    }

    public GaugesTypes getType(final String code) {
        return (LOOK_UP.containsKey(code)) ? LOOK_UP.get(code) : UNKNOWN;
    }
}
