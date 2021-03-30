package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
public class KubernetesGreetingResourceIT extends GreetingResourceIT {

    @Test
    public void shouldInjectKubectlClient(KubectlClient client) {
        assertNotNull(client);
    }
}
