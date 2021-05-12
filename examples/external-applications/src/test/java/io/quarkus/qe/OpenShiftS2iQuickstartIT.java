package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.QuarkusS2IBuild)
public class OpenShiftS2iQuickstartIT {

    @QuarkusApplication(sourceS2iRepositoryUri = "https://github.com/quarkusio/quarkus-quickstarts.git", sourceS2iGitRef = "1.11", sourceS2iContextDir = "getting-started")
    static final RestService app = new RestService();

    @Test
    public void test() {
        app.given()
                .get("/hello")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
