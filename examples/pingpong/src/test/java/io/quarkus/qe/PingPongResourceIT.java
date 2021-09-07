package io.quarkus.qe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;

@QuarkusScenario
public class PingPongResourceIT {

    @Test
    public void shouldPingPongWorks() {
        given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }
}
