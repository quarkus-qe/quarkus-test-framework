package io.quarkus.qe;

import io.quarkus.test.scenarios.KubernetesDeploymentStrategy;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario(deployment = KubernetesDeploymentStrategy.UsingContainerRegistry)
public class KubernetesUsingContainerRegistryPingPongResourceIT extends PingPongResourceIT {
}
