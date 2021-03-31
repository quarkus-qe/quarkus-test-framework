package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class PingPongResourceIT {

    @QuarkusApplication
    static final RestService ping = new RestService();

    @Test
    public void shouldPingPongWorks() {
        ping.given().get("/ping/pong").then().statusCode(HttpStatus.SC_OK).and().body(is("pingpong"));
    }
}
