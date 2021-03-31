package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceIT {
    @QuarkusApplication(ssl = true)
    static final RestService app = new RestService();

    @Test
    public void shouldSayHelloWorld() {
        app.https().given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello World!"));
    }

}
