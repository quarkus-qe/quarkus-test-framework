package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.cloneRepository;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ExtensionOpenShiftUsingDockerBuildStrategyGitRepositoryQuarkusApplicationManagedResource
        extends ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource {

    public ExtensionOpenShiftUsingDockerBuildStrategyGitRepositoryQuarkusApplicationManagedResource(
            ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public void onPreBuild() {
        super.onPreBuild();
        cloneRepository(getModel());
    }

    @Override
    protected void withAdditionalArguments(List<String> args) {
        String[] mvnArgs = StringUtils.split(getModel().getMavenArgsWithVersion(), " ");
        args.addAll(Arrays.asList(mvnArgs));
    }

    @Override
    protected void cloneProjectToServiceAppFolder() {
        // we are cloning app from git repo
    }

    private GitRepositoryQuarkusApplicationManagedResourceBuilder getModel() {
        return (GitRepositoryQuarkusApplicationManagedResourceBuilder) model;
    }
}
