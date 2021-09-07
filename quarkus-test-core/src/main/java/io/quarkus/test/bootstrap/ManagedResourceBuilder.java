package io.quarkus.test.bootstrap;

import java.lang.annotation.Annotation;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * @return computed property that depends on the managed resource builder implementation.
     */
    default String getComputedProperty(String property) {
        return StringUtils.EMPTY;
    }
}
