package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
@DisabledOnOs(OS.WINDOWS) // cloning fails on Windows due to long file names
public class SSLRequestIT {
    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkus-qe/quarkus-test-framework.git", branch = "1.7.z", contextDir = "misc/test-applications/git-repo-app")
    static final RestService app = new RestService();

    @Test
    public void shouldSendExternalRequests() {
        Response response = app.given().get("/external-https");
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        Assertions.assertEquals("200", response.body().asString());
    }
}
