package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
public class OpenShiftGreetingResourceIT extends GreetingResourceIT {
    @Test
    public void shouldInjectOpenShiftClient(OpenShiftClient client) {
        assertNotNull(client);
    }
}
