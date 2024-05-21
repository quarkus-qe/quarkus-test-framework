package io.quarkus.test.scenarios;

import java.util.function.Function;

import io.quarkus.test.services.quarkus.ContainerRegistryKubernetesQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ExtensionKubernetesQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.KubernetesQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder;

/**
 * Kubernetes Deployment strategies.
 */
public enum KubernetesDeploymentStrategy {
    /**
     * Will build the Quarkus app image and push it into a Container Registry to be accessed by Kubernetes to deploy the app.
     */
    UsingContainerRegistry(ContainerRegistryKubernetesQuarkusApplicationManagedResource::new),
    /**
     * Will use the OpenShift Quarkus extension to build and deploy into Kubernetes.
     */
    UsingKubernetesExtension(ExtensionKubernetesQuarkusApplicationManagedResource::new);

    private final Function<ProdQuarkusApplicationManagedResourceBuilder, KubernetesQuarkusApplicationManagedResource<?>> sup;

    KubernetesDeploymentStrategy(
            Function<ProdQuarkusApplicationManagedResourceBuilder, KubernetesQuarkusApplicationManagedResource<?>> supplier) {
        this.sup = supplier;
    }

    public KubernetesQuarkusApplicationManagedResource<?> getManagedResource(
            ProdQuarkusApplicationManagedResourceBuilder builder) {
        return sup.apply(builder);
    }
}
