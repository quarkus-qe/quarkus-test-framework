package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.cloneRepository;

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
    }

    private GitRepositoryQuarkusApplicationManagedResourceBuilder getModel() {
        return (GitRepositoryQuarkusApplicationManagedResourceBuilder) model;
    }
}
