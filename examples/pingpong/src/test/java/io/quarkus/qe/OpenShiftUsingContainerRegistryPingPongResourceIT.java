package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.EnabledIfOpenShiftScenarioPropertyIsTrue;

@EnabledIfOpenShiftScenarioPropertyIsTrue
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
public class OpenShiftUsingContainerRegistryPingPongResourceIT extends PingPongResourceIT {
}
