package io.quarkus.test.quarkus;

import io.quarkus.test.ManagedResource;
import io.quarkus.test.ServiceContext;

public interface QuarkusApplicationManagedResourceBinding {
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
    ManagedResource init(QuarkusApplicationManagedResourceBuilder builder);
}
