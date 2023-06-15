package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;
import io.restassured.specification.RequestSpecification;

@QuarkusScenario
public class HttpIT {

    private final RequestSpecification spec = given();

    @Test
    public void shouldSayHelloWorld() {
        untilAsserted(() -> spec.get("/greeting")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Hello World!")));
    }
}
