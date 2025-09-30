package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.KubernetesScenario;

@Disabled("https://github.com/quarkus-qe/quarkus-test-framework/issues/1708")
@KubernetesScenario
public class KubernetesGreetingResourceIT extends GreetingResourceIT {
}
