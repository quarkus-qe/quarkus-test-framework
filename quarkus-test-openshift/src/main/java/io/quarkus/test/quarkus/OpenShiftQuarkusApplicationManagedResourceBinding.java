package io.quarkus.test.quarkus;

import io.quarkus.test.ManagedResource;
import io.quarkus.test.ServiceContext;
import io.quarkus.test.annotation.OpenShiftTest;

public class OpenShiftQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftTest.class);
    }

    @Override
    public ManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        return new OpenShiftQuarkusApplicationManagedResource(builder);
    }

}
