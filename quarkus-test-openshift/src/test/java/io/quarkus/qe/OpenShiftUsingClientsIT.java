package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
public class OpenShiftUsingClientsIT {

    @Inject
    static OpenShiftClient clientAsStaticInstance;

    @Test
    public void shouldInjectOpenShiftClientAsStaticInstance() {
        assertNotNull(clientAsStaticInstance);
    }

    @Test
    public void shouldInjectOpenShiftClientAsField(OpenShiftClient clientAsField) {
        assertNotNull(clientAsField);
    }
}
