package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
@Disabled("https://github.com/quarkusio/quarkus/issues/38018")
public class OpenShiftUsingExtensionBuildTimeGreetingResourceIT extends BuildTimeGreetingResourceIT {
}
