package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class ContainerRegistryOpenShiftQuarkusApplicationManagedResourceBinding
        implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        OpenShiftScenario annotation = context.getTestContext().getRequiredTestClass().getAnnotation(OpenShiftScenario.class);
        return annotation != null && annotation.deployment() == OpenShiftDeploymentStrategy.UsingContainerRegistry;
    }

    @Override
    public QuarkusManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        return new ContainerRegistryOpenShiftQuarkusApplicationManagedResource(builder);
    }

}
