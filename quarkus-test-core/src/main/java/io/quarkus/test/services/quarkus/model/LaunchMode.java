package io.quarkus.test.services.quarkus.model;

public enum LaunchMode {
    LEGACY_JAR("legacy-jar"),
    NATIVE("native"),
    JVM("jvm"),
    DEV("dev");

    private final String name;

    LaunchMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
