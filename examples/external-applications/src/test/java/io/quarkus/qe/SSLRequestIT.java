package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class SSLRequestIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/fedinskiy/reproducer.git", branch = "framework-ssl-anchor")
    static final RestService app = new RestService();

    @Test
    public void shouldSendExternalRequests() {
        Response response = app.given().get("/external-https");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        Assertions.assertEquals("200", response.body().asString());
    }
}
