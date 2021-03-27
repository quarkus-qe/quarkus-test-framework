package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceUsingRuntimePropertiesIT {

    private static final String JOSE_NAME = "jose";
    private static final String MANUEL_NAME = "manuel";

    @QuarkusApplication
    static final Service joseApp = new Service(JOSE_NAME).withProperties("jose.properties");

    @QuarkusApplication
    static final Service manuelApp = new Service(MANUEL_NAME).withProperties("manuel.properties");

    @Test
    public void shouldSayJose() {
        joseApp.restAssured().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + JOSE_NAME));
    }

    @Test
    public void shouldSayManuel() {
        manuelApp.restAssured().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + MANUEL_NAME));
    }

}
