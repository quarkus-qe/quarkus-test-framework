package io.quarkus.test.services.quarkus.utils;

import io.quarkus.test.services.Dependency;

public final class Dependencies {
    private Dependencies() {
        super();
    }

    public static String shortGAV(Dependency dependency) {
        //io.quarkus:quarkus-ide-config:jar:999-SNAPSHOT
        return dependency.groupId() + ":" + dependency.artifactId() + ":" + dependency.version();
    }
}
