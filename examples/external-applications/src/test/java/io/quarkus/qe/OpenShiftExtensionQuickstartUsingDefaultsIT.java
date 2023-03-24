package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
public class OpenShiftExtensionQuickstartUsingDefaultsIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/apache/camel-quarkus-examples.git", contextDir = "file-bindy-ftp", mavenArgs = "-Dopenshift")
    static final RestService app = new RestService();

    @Test
    public void test() {
        app.given()
                .get("/q/health")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
