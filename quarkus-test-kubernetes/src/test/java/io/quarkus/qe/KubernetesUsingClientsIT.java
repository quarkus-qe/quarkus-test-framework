package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
public class KubernetesUsingClientsIT {

    @Inject
    static KubernetesClient clientAsStaticInstance;

    @Test
    public void shouldInjectKubernetesClientAsStaticInstance() {
        assertNotNull(clientAsStaticInstance);
    }

    @Test
    public void shouldInjectKubernetesClientAsField(KubernetesClient clientAsField) {
        assertNotNull(clientAsField);
    }
}
