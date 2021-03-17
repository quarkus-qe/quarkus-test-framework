package io.quarkus.test.containers;

import io.quarkus.test.ManagedResource;
import io.quarkus.test.ServiceContext;
import io.quarkus.test.annotation.OpenShiftTest;

public class OpenShiftContainerManagedResourceBinding implements ContainerManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftTest.class);
    }

    @Override
    public ManagedResource init(ContainerManagedResourceBuilder builder) {
        return new OpenShiftContainerManagedResource(builder);
    }

}
