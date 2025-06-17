package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.KubernetesScenario;

@Disabled("https://github.com/quarkusio/quarkus/issues/48198")
@KubernetesScenario
public class KubernetesRemoteDevModeGreetingResourceIT extends RemoteDevModeGreetingResourceIT {
}
