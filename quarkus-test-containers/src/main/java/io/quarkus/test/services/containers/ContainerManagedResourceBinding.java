package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;

public interface ContainerManagedResourceBinding {
    /**
     * @param context
     * @return if the current managed resource applies for the current context.
     */
    boolean appliesFor(ServiceContext context);

    /**
     * Init and return the managed resource for the current context.
     *
     * @param builder
     * @return
     */
    ManagedResource init(ContainerManagedResourceBuilder builder);
}
