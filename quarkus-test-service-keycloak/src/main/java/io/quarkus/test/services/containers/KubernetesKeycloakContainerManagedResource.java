/*  File: KubernetesKeycloakContainerManagedResource.java
    Author: Georgii Troitskii (xtroit00)
    Date: 9.5.2024
*/

package io.quarkus.test.services.containers;

public class KubernetesKeycloakContainerManagedResource extends KubernetesContainerManagedResource {

    protected KubernetesKeycloakContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(model);
    }
}
