package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class PingPongResourceIT {

    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingApp = new RestService();

    @QuarkusApplication(classes = PongResource.class)
    static final RestService pongApp = new RestService();

    @QuarkusApplication
    static final RestService pingPongApp = new RestService();

    @Test
    public void shouldPingWorks() {
        pingApp.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.given().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPongWorks() {
        pongApp.given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
        pongApp.given().get("/ping").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPingPongWorks() {
        pingPongApp.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingPongApp.given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }

}
