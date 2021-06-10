package io.quarkus.test.bootstrap;

import java.lang.annotation.Annotation;

public interface ManagedResourceBuilder {

    /**
     * Build the resource using the service context.
     */
    ManagedResource build(ServiceContext context);

    /**
     * (Optional) Init managed resource builder using the metadata from the annotation.
     *
     * @param annotation metadata
     */
    default void init(Annotation annotation) {

    }
}
