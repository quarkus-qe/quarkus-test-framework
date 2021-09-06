package io.quarkus.qe;

import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@KubernetesScenario
@DisabledOnNative
public class KubernetesRemoteDevGreetingResourceIT extends RemoteDevGreetingResourceIT {
}
