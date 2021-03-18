package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.QuarkusScenario;
import io.quarkus.test.Service;
import io.quarkus.test.annotation.QuarkusApplication;

@QuarkusScenario
public class PingPongResourceIT {

    @QuarkusApplication(classes = PingResource.class)
    static final Service pingApp = new Service("ping");

    @QuarkusApplication(classes = PongResource.class)
    static final Service pongApp = new Service("pong");

    @QuarkusApplication
    static final Service pingPongApp = new Service("pingpong");

    @Test
    public void shouldPingWorks() {
        pingApp.restAssured().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.restAssured().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPongWorks() {
        pongApp.restAssured().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
        pongApp.restAssured().get("/ping").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPingPongWorks() {
        pingPongApp.restAssured().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingPongApp.restAssured().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }

}
