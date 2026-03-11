package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.specification.RequestSpecification;

public abstract class BaseHttpServiceIT {

    private final RequestSpecification spec = given();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("user", "testuser")
            .withSecretProperty("password", "super_secret_password");

    @Test
    public void shouldSayHelloWorld() {
        untilAsserted(() -> spec.get("/greeting")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Hello World!")));
    }

    @Test
    public void shouldUseSecretProperty() {
        untilAsserted(() -> spec.get("/greeting/credentials")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("User: testuser"))
                .body(containsString("Password: super_secret_password")));
    }
}
