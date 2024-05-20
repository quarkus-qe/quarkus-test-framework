package io.quarkus.test.scenarios;

import java.util.function.Function;

import io.quarkus.test.services.quarkus.BuildOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ContainerRegistryOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ExtensionOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder;

/**
 * OpenShift Deployment strategies.
 */
public enum OpenShiftDeploymentStrategy {
    /**
     * Will push the artifacts into OpenShift and build the image that will be used to run the pods.
     */
    Build(BuildOpenShiftQuarkusApplicationManagedResource::new),
    /**
     * Will build the Quarkus app image and push it into a Container Registry to be accessed by OpenShift to deploy the app.
     */
    UsingContainerRegistry(ContainerRegistryOpenShiftQuarkusApplicationManagedResource::new),
    /**
     * Will use the OpenShift Quarkus extension to build and deploy into OpenShift.
     */
    UsingOpenShiftExtension(ExtensionOpenShiftQuarkusApplicationManagedResource::new),
    /**
     * Will use the OpenShift Quarkus extension to build within Docker and then deploy into OpenShift.
     */
    UsingOpenShiftExtensionAndDockerBuildStrategy(
            ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource::new);

    private final Function<ProdQuarkusApplicationManagedResourceBuilder, OpenShiftQuarkusApplicationManagedResource<?>> sup;

    OpenShiftDeploymentStrategy(
            Function<ProdQuarkusApplicationManagedResourceBuilder, OpenShiftQuarkusApplicationManagedResource<?>> supplier) {
        this.sup = supplier;
    }

    public OpenShiftQuarkusApplicationManagedResource<?> getManagedResource(
            ProdQuarkusApplicationManagedResourceBuilder builder) {
        return sup.apply(builder);
    }
}
