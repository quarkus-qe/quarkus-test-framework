package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class LocalTlsRegistryIT {

    @QuarkusApplication(certificates = @Certificate(configureManagementInterface = true, configureKeystore = true))
    static final RestService service = new RestService()
            .withProperty("quarkus.management.port", "9003");

    @Test
    public void greeting() {
        Response response = service.given().get("/ping");
        assertEquals(200, response.statusCode());
        assertEquals("pong", response.body().asString());
    }

    @Test
    public void tls() {
        var statusCode = service.mutinyHttps().get("/q/health").sendAndAwait().statusCode();
        assertEquals(200, statusCode);
    }
}
