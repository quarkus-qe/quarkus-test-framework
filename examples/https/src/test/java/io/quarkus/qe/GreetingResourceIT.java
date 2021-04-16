package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
// SSL is not working when running on Native. It needs to be investigated.
@DisabledOnNative
public class GreetingResourceIT {
    @QuarkusApplication(ssl = true)
    static final RestService app = new RestService();

    @Test
    public void shouldSayHelloWorld() {
        untilAsserted(() -> app.https().given().relaxedHTTPSValidation().get("/greeting")
                .then().statusCode(HttpStatus.SC_OK).body(is("Hello World!")));
    }

}
