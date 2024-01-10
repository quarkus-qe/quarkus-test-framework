package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import org.junit.jupiter.api.Disabled;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
@Disabled("https://github.com/quarkusio/quarkus/issues/38018")
public class OpenShiftUsingExtensionPingPongResourceIT extends PingPongResourceIT {
}
