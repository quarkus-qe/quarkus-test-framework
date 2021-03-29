package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ManagedResource;

public interface QuarkusManagedResource extends ManagedResource {
    default boolean needsBuildArtifact() {
        return true;
    }

    default void validate() {
    }
}
