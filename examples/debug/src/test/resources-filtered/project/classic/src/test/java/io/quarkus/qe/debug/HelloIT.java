package io.quarkus.qe.debug;

import io.quarkus.test.scenarios.QuarkusScenario;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusScenario
public class HelloIT {

    @Test
    public void test() {
        RestAssured
                .get("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.is("hello"));
    }

}
