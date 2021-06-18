package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@DisabledOnQuarkusVersion(version = "9\\..*", reason = "999-SNAPSHOT is not available in the Maven repositories in OpenShift")
@OpenShiftScenario
public class OpenShiftS2iQuickstartUsingUberJarIT {

    /**
     * Package type is set in the custom template.
     */
    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", contextDir = "getting-started", branch = "1.11")
    static final RestService appuberjar = new RestService();

    @Test
    public void test() {
        appuberjar.given()
                .get("/hello")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
