package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
@DisabledOnNative
public class DevModeGreetingResourceIT {
    @DevModeQuarkusApplication(ssl = true)
    static DevModeQuarkusService app = new DevModeQuarkusService();

    @Test
    public void shouldOpenDevUi() {
        app.given().get("/q/dev").then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void shouldOpenHttpsDevUi() {
        app.relaxedHttps().get("/q/dev").then().statusCode(HttpStatus.SC_OK);
    }
}
