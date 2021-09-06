package io.quarkus.test.services.quarkus;

import java.nio.file.Path;

public abstract class ArtifactQuarkusApplicationManagedResourceBuilder extends QuarkusApplicationManagedResourceBuilder {
    protected abstract Path getArtifact();
}
