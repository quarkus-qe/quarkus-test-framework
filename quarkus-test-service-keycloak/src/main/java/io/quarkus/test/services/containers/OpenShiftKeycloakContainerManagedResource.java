package io.quarkus.test.services.containers;

public class OpenShiftKeycloakContainerManagedResource extends OpenShiftContainerManagedResource {

    protected OpenShiftKeycloakContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(model);
    }
}
