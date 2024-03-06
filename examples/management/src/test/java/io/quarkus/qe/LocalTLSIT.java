package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
// todo Merge with LocalIT when https://github.com/quarkusio/quarkus/issues/32225 is fixed
public class LocalTLSIT {

    @QuarkusApplication
    static final RestService service = new RestService()
            .withProperty("quarkus.management.port", "9003")
            .withProperty("quarkus.management.ssl.certificate.key-store-file", "META-INF/resources/server.keystore")
            .withProperty("quarkus.management.ssl.certificate.key-store-file-type", "JKS")
            .withProperty("quarkus.management.ssl.certificate.key-store-password", "password");

    @Test
    public void greeting() {
        Response response = service.given().get("/ping");
        assertEquals(200, response.statusCode());
        assertEquals("pong", response.body().asString());
    }

    @Test
    public void tls() {
        service.management()
                .relaxedHTTPSValidation()
                .get("q/health").then().statusCode(HttpStatus.SC_OK);
    }
}
