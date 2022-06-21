package io.quarkus.test.services.containers;

public class OpenShiftKeycloakContainerManagedResource extends OpenShiftContainerManagedResource {

    private final KeycloakContainerManagedResourceBuilder model;

    protected OpenShiftKeycloakContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }
}
