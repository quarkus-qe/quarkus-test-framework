package io.quarkus.test.scenarios;

/**
 * OpenShift Deployment strategies. *
 */
public enum OpenShiftDeploymentStrategy {
    /**
     * Will push the artifacts into OpenShift and build the image that will be used to run the pods.
     */
    Build,
    /**
     * Will build the Quarkus app image and push it into a Container Registry to be accessed by OpenShift to deploy the app.
     */
    UsingContainerRegistry,
    /**
     * Will use the OpenShift Quarkus extension to build and deploy into OpenShift.
     */
    UsingOpenShiftExtension,
    /**
     * Will use the OpenShift Quarkus extension to build within Docker and then deploy into OpenShift.
     */
    UsingOpenShiftExtensionAndDockerBuildStrategy

}
