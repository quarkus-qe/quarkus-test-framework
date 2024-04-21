package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.KubernetesScenario;

public class KubernetesKeycloakContainerManagedResourceBinding implements KeycloakContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(KeycloakContainerManagedResourceBuilder builder) {
        return builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public ManagedResource init(KeycloakContainerManagedResourceBuilder builder) {
        return new KubernetesKeycloakContainerManagedResource(builder);
    }
}
