package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import io.quarkus.test.bootstrap.ManagedResource;

public class KubernetesKeycloakContainerManagedResourceBinding implements KeycloakContainerManagedResourceBinding {
    @Override
    public boolean appliesFor(KeycloakContainerManagedResourceBuilder builder) {
        Annotation[] annotations = builder.getContext().getTestContext().getRequiredTestClass().getAnnotations();
        return Arrays.stream(annotations)
                .anyMatch(annotation -> annotation.annotationType().getName()
                        .equals("io.quarkus.test.scenarios.KubernetesScenario"));
    }

    @Override
    public ManagedResource init(KeycloakContainerManagedResourceBuilder builder) {
        return new KubernetesKeycloakContainerManagedResource(builder);
    }
}
