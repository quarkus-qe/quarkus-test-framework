package io.quarkus.qe;

import static io.quarkus.test.services.Certificate.Format.JKS;
import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@EnabledOnOs(OS.WINDOWS)
public class WindowsGreetingResourceIT {
    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, format = JKS))
    static final RestService app = new RestService();

    @Test
    public void shouldSayHelloWorld() {
        untilAsserted(() -> app.https().given().relaxedHTTPSValidation().get("/greeting")
                .then().statusCode(HttpStatus.SC_OK).body(is("Hello World!")));
    }

}
