package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceUsingRuntimePropertiesIT {

    static final String JOSE_NAME = "jose";
    static final String MANUEL_NAME = "manuel";

    @QuarkusApplication
    static final RestService joseApp = new RestService().withProperty(ValidateCustomProperty.CUSTOM_PROPERTY, JOSE_NAME);
    @QuarkusApplication
    static final RestService manuelApp = new RestService().withProperty(ValidateCustomProperty.CUSTOM_PROPERTY, MANUEL_NAME);

    @Test
    public void shouldSayJose() {
        joseApp.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + JOSE_NAME));
    }

    @Test
    public void shouldSayManuel() {
        manuelApp.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello, I'm " + MANUEL_NAME));
    }

    @DisabledOnNative
    @Test
    public void shouldLoadResources() {
        joseApp.given().get("/greeting/file").then().statusCode(HttpStatus.SC_OK).body(is("found!"));
        manuelApp.given().get("/greeting/file").then().statusCode(HttpStatus.SC_OK).body(is("found!"));
    }

}
