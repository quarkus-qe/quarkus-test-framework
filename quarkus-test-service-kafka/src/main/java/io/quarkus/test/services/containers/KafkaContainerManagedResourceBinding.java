package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;

public interface KafkaContainerManagedResourceBinding {
    boolean appliesFor(KafkaContainerManagedResourceBuilder builder);

    ManagedResource init(KafkaContainerManagedResourceBuilder builder);
}
