package io.quarkus.test.metrics;

public enum GaugesTypes {
    TESTS_TOTAL("tests_total"),
    TESTS_SUCCEED("tests_succeed"),
    TESTS_IGNORE("tests_ignored"),
    TESTS_FAILED("tests_failed");

    private String code;

    GaugesTypes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
