package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;

public interface JaegerContainerManagedResourceBinding {
    boolean appliesFor(JaegerContainerManagedResourceBuilder builder);

    ManagedResource init(JaegerContainerManagedResourceBuilder builder);
}
