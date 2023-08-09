package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@Disabled("https://github.com/quarkusio/quarkus/issues/35288")
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy)
public class OpenShiftUsingExtensionAndDockerBuildAndServerlessPingPongResourceIT extends PingPongResourceIT {
}
