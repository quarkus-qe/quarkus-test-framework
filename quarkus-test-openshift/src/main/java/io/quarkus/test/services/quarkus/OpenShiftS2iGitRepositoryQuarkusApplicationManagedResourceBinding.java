package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftS2iGitRepositoryQuarkusApplicationManagedResourceBinding
        implements GitRepositoryQuarkusApplicationManagedResourceBinding {
    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public QuarkusManagedResource init(GitRepositoryQuarkusApplicationManagedResourceBuilder builder) {
        return new OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(builder);
    }
}
