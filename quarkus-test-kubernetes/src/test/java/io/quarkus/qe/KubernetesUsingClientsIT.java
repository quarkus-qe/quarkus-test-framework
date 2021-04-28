package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
public class KubernetesUsingClientsIT {

    @Inject
    static KubectlClient clientAsStaticInstance;

    @Test
    public void shouldInjectKubernetesClientAsStaticInstance() {
        assertNotNull(clientAsStaticInstance);
    }

    @Test
    public void shouldInjectKubernetesClientAsField(KubectlClient clientAsField) {
        assertNotNull(clientAsField);
    }
}
