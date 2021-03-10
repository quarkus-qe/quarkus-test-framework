package io.quarkus.test.containers;

import io.quarkus.test.ManagedResource;
import io.quarkus.test.ServiceContext;

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
	 * @param context
	 * @return
	 */
	ManagedResource init(ContainerManagedResourceBuilder builder, ServiceContext context);
}
