package io.quarkus.test.utils;

public final class JavaUtils {

    private JavaUtils() {

    }

    public static boolean isRunningSemeruJdk() {
        return System.getProperty("java.runtime.name").toLowerCase().contains("semeru");
    }
}
