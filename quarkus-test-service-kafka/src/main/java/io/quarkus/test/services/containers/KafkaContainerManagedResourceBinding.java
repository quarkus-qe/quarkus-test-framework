package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;

public interface KafkaContainerManagedResourceBinding {
    boolean appliesFor(ServiceContext context);

    ManagedResource init(KafkaContainerManagedResourceBuilder builder);
}
