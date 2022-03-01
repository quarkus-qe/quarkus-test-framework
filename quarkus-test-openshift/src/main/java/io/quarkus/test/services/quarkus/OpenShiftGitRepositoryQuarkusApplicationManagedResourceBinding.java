package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftGitRepositoryQuarkusApplicationManagedResourceBinding
        implements GitRepositoryQuarkusApplicationManagedResourceBinding {
    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public QuarkusManagedResource init(GitRepositoryQuarkusApplicationManagedResourceBuilder builder) {
        OpenShiftScenario annotation = builder.getContext().getTestContext().getRequiredTestClass()
                .getAnnotation(OpenShiftScenario.class);
        if (annotation.deployment() == OpenShiftDeploymentStrategy.UsingOpenShiftExtension) {
            return new ExtensionOpenShiftGitRepositoryQuarkusApplicationManagedResource(builder);
        } else if (annotation.deployment() == OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy) {
            return new ExtensionOpenShiftUsingDockerBuildStrategyGitRepositoryQuarkusApplicationManagedResource(builder);
        } else if (annotation.deployment() == OpenShiftDeploymentStrategy.UsingContainerRegistry) {
            return new ContainerRegistryOpenShiftGitRepositoryQuarkusApplicationManagedResource(builder);
        } else if (annotation.deployment() == OpenShiftDeploymentStrategy.Build) {
            return new OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(builder);
        }
        throw new IllegalStateException(String.format("Annotation %s with GitRepository is not supported combination",
                annotation.deployment().getClass().getName()));
    }
}
