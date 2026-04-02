package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
@Disabled("https://github.com/quarkusio/quarkus/issues/53407")
public class KubernetesRemoteDevModeGreetingResourceIT extends RemoteDevModeGreetingResourceIT {
}
