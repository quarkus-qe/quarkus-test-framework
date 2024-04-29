package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@QuarkusScenario
@DisabledOnNative(reason = "This is to verify uber-jar, so it does not make sense on Native")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows does not support long file paths")
public class QuickstartUsingUsingUberJarIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", branch = "3.8", contextDir = "getting-started", mavenArgs = "-Dquarkus.package.type=uber-jar -DskipTests=true -Dquarkus.platform.group-id=${QUARKUS_PLATFORM_GROUP-ID} -Dquarkus.platform.version=${QUARKUS_PLATFORM_VERSION}")
    static final RestService app = new RestService();

    @Test
    public void test() throws InterruptedException {
        app.given()
                .get("/hello")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
