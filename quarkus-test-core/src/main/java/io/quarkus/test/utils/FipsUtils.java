package io.quarkus.test.utils;

public final class FipsUtils {

    /**
     * We set environment variable "FIPS" to "fips" in our Jenkins jobs when FIPS are enabled.
     */
    private static final String FIPS_ENABLED = "fips";

    private FipsUtils() {

    }

    public static boolean isFipsEnabledEnvironment() {
        return FIPS_ENABLED.equalsIgnoreCase(System.getenv().get("FIPS"));
    }
}
