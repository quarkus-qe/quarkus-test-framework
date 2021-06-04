package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.EnabledIfOpenShiftScenarioPropertyIsTrue;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
@EnabledIfOpenShiftScenarioPropertyIsTrue
public class OpenShiftUsingContainerRegistryPingPongResourceIT extends PingPongResourceIT {
}
