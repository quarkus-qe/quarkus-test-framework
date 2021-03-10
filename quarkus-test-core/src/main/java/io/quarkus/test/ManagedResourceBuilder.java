package io.quarkus.test;

import java.lang.annotation.Annotation;

public interface ManagedResourceBuilder {

	void init(Annotation annotation);

	/**
	 * Build the resource using the service context.
	 */
	ManagedResource build(ServiceContext context);
}
