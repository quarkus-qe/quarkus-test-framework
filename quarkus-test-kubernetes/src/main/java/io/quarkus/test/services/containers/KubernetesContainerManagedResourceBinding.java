package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.KubernetesScenario;

public class KubernetesContainerManagedResourceBinding implements ContainerManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public ManagedResource init(ContainerManagedResourceBuilder builder) {
        return new KubernetesContainerManagedResource(builder);
    }

}
