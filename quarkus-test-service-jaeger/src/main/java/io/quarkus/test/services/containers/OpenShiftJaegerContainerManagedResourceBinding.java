package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftJaegerContainerManagedResourceBinding implements JaegerContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(JaegerContainerManagedResourceBuilder builder) {
        return builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(JaegerContainerManagedResourceBuilder builder) {
        return new OpenShiftJaegerContainerManagedResource(builder);
    }
}
