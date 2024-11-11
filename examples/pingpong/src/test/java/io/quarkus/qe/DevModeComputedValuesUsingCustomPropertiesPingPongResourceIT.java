package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeComputedValuesUsingCustomPropertiesPingPongResourceIT {

    @DevModeQuarkusApplication(properties = "custom.properties")
    static final RestService pingpong = new RestService();

    @Test
    public void shouldGetComputedValuesFromCustomPropertiesFile() {
        assertEquals("C", pingpong.getProperty("property.exists.only.in.custom.properties").orElseThrow());
    }

}
