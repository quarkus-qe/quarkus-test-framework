package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftKeycloakContainerManagedResourceBinding implements KeycloakContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(KeycloakContainerManagedResourceBuilder builder) {
        return builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(KeycloakContainerManagedResourceBuilder builder) {
        return new OpenShiftKeycloakContainerManagedResource(builder);
    }
}
