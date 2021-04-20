package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;

public interface AmqContainerManagedResourceBinding {
    boolean appliesFor(AmqContainerManagedResourceBuilder builder);

    ManagedResource init(AmqContainerManagedResourceBuilder builder);
}
