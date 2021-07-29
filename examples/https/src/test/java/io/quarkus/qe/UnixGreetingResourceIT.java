package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@DisabledOnOs(OS.WINDOWS)
public class UnixGreetingResourceIT {
    @QuarkusApplication(ssl = true)
    static final RestService app = new RestService()
            .withProperties("unix.application.properties");

    @Test
    public void shouldSayHelloWorld() {
        untilAsserted(() -> app.https().given().relaxedHTTPSValidation().get("/greeting")
                .then().statusCode(HttpStatus.SC_OK).body(is("Hello World!")));
    }

}
