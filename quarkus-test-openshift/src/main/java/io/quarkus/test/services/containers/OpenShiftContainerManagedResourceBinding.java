package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftContainerManagedResourceBinding implements ContainerManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(ContainerManagedResourceBuilder builder) {
        return new OpenShiftContainerManagedResource(builder);
    }

}
