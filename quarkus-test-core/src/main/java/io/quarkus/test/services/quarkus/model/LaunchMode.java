package io.quarkus.test.services.quarkus.model;

public enum LaunchMode {
    FAST_JAR("fast-jar"),
    NATIVE("native"),
    JVM("jvm");

    private final String name;

    LaunchMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
