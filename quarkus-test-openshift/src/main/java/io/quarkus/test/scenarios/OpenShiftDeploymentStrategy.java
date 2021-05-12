package io.quarkus.test.scenarios;

import io.quarkus.test.services.quarkus.BuildOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ContainerRegistryOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ExtensionOpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.OpenShiftQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.QuarkusSourceS2iBuildApplicationManagedResource;

/**
 * OpenShift Deployment strategies.
 */
public enum OpenShiftDeploymentStrategy {
    /**
     * Will push the artifacts into OpenShift and build the image that will be used to run the pods.
     */
    Build(BuildOpenShiftQuarkusApplicationManagedResource.class),
    /**
     * This build strategy will use the source S2I build strategy to build the container with the running Quarkus
     * application.
     */
    QuarkusS2IBuild(QuarkusSourceS2iBuildApplicationManagedResource.class),
    /**
     * Will build the Quarkus app image and push it into a Container Registry to be accessed by OpenShift to deploy the app.
     */
    UsingContainerRegistry(ContainerRegistryOpenShiftQuarkusApplicationManagedResource.class),
    /**
     * Will use the OpenShift Quarkus extension to build and deploy into OpenShift.
     */
    UsingOpenShiftExtension(ExtensionOpenShiftQuarkusApplicationManagedResource.class),
    /**
     * Will use the OpenShift Quarkus extension to build within Docker and then deploy into OpenShift.
     */
    UsingOpenShiftExtensionAndDockerBuildStrategy(
            ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource.class);

    private final Class<? extends OpenShiftQuarkusApplicationManagedResource> strategyClass;

    OpenShiftDeploymentStrategy(Class<? extends OpenShiftQuarkusApplicationManagedResource> strategyClass) {
        this.strategyClass = strategyClass;
    }

    public Class<? extends OpenShiftQuarkusApplicationManagedResource> getStrategyClass() {
        return strategyClass;
    }
}
