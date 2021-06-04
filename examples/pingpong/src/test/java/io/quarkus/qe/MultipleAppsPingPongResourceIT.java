package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.QuarkusApplication;

@DisabledOnNative(reason = "Due to high native build execution time for the three Quarkus services")
@QuarkusScenario
public class MultipleAppsPingPongResourceIT {

    @QuarkusApplication(classes = PingResource.class)
    static final RestService ping = new RestService();

    @QuarkusApplication(classes = PongResource.class)
    static final RestService pong = new RestService();

    @QuarkusApplication
    static final RestService pingpong = new RestService();

    @Test
    public void shouldPingWorks() {
        ping.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        ping.given().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPongWorks() {
        pong.given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
        pong.given().get("/ping").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void shouldPingPongWorks() {
        pingpong.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingpong.given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }

}
