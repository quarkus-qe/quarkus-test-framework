package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftGrafanaContainerManagedResourceBinding implements GrafanaContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(GrafanaContainerManagedResourceBuilder builder) {
        return builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(GrafanaContainerManagedResourceBuilder builder) {
        return new OpenShiftGrafanaContainerManagedResource(builder);
    }
}
