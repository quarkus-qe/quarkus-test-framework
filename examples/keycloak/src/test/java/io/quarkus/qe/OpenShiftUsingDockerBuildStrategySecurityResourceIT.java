package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy)
@Disabled("https://github.com/quarkusio/quarkus/issues/38018")
public class OpenShiftUsingDockerBuildStrategySecurityResourceIT extends SecurityResourceIT {
}
