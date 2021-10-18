package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
public class OpenShiftUsingContainerRegistryPingPongResourceIT extends PingPongResourceIT {
}
