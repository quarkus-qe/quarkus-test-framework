package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class LocalIT {

    @QuarkusApplication
    static final RestService app = new RestService();

    @QuarkusApplication
    static final RestService custom = new RestService()
            .withProperty("quarkus.management.port", "9002");

    @QuarkusApplication
    static final RestService tls = new RestService()
            .withProperty("quarkus.management.port", "9003")
            .withProperty("quarkus.management.ssl.certificate.key-store-file", "META-INF/resources/server.keystore")
            .withProperty("quarkus.management.ssl.certificate.key-store-password", "password");

    @QuarkusApplication
    static final RestService unmanaged = new RestService()
            .withProperty("quarkus.management.enabled", "false");

    @Test
    public void greeting() {
        for (RestService service : Arrays.asList(app, custom, tls, unmanaged)) {
            Response response = service.given().get("/ping");
            assertEquals(200, response.statusCode());
            assertEquals("pong", response.body().asString());
        }
    }

    @Test
    public void health() {
        unmanaged.given().get("q/health").then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void managed() {
        app.management().get("q/health").then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void customPort() {
        custom.management()
                .get("q/health").then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void tls() {
        tls.management()
                .relaxedHTTPSValidation()
                .get("q/health").then().statusCode(HttpStatus.SC_OK);
    }
}
