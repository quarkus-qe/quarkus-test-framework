package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftAmqContainerManagedResourceBinding implements AmqContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(AmqContainerManagedResourceBuilder builder) {
        return builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(AmqContainerManagedResourceBuilder builder) {
        return new AmqOpenShiftContainerManagedResource(builder);
    }
}
