package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;

public interface GrafanaContainerManagedResourceBinding {
    boolean appliesFor(GrafanaContainerManagedResourceBuilder builder);

    ManagedResource init(GrafanaContainerManagedResourceBuilder builder);
}
