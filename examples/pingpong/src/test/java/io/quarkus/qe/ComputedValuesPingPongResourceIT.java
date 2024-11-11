package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class ComputedValuesPingPongResourceIT {

    @QuarkusApplication
    static final RestService pingpong = new RestService();

    @Test
    public void shouldGetComputedValues() {
        assertFalse(pingpong.getProperty("property.not.exists").isPresent());
        assertEquals("A", pingpong.getProperty("property.without.profile").orElseThrow());
        assertEquals("B", pingpong.getProperty("property.with.profile").orElseThrow());
    }

}
