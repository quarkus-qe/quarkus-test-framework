package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.cloneRepository;
import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.mavenBuild;

public class ContainerRegistryOpenShiftGitRepositoryQuarkusApplicationManagedResource
        extends ContainerRegistryOpenShiftQuarkusApplicationManagedResource {

    public ContainerRegistryOpenShiftGitRepositoryQuarkusApplicationManagedResource(
            ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public void onPreBuild() {
        super.onPreBuild();

        cloneRepository(getModel());
        mavenBuild(getModel());
    }

    private GitRepositoryQuarkusApplicationManagedResourceBuilder getModel() {
        return (GitRepositoryQuarkusApplicationManagedResourceBuilder) model;
    }
}
