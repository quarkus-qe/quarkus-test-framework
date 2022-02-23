package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@DisabledOnNative(reason = "This scenario is using uber-jar, so it's incompatible with Native")
@QuarkusScenario
public class TodoDemoIT {
    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/todo-demo-app.git", mavenArgs = "-Dquarkus.package.type=uber-jar -DskipTests=true -Dquarkus.platform.group-id=${QUARKUS_PLATFORM_GROUP-ID} -Dquarkus.platform.version=${QUARKUS_VERSION}")
    static final RestService app = new RestService();

    @Test
    public void verify() {
        app.given()
                .get()
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}