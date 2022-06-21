package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;

public interface KeycloakContainerManagedResourceBinding {
    boolean appliesFor(KeycloakContainerManagedResourceBuilder builder);

    ManagedResource init(KeycloakContainerManagedResourceBuilder builder);
}
