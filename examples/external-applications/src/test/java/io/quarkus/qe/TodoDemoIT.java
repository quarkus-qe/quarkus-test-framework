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
    private static final String REPO = "https://github.com/quarkusio/todo-demo-app.git";
    private static final String DEFAULT_ARGS = "-DskipTests=true -Dquarkus.platform.group-id=${QUARKUS_PLATFORM_GROUP-ID} -Dquarkus.platform.version=${QUARKUS_VERSION} ";
    private static final String UBER = "-Dquarkus.package.type=uber-jar ";

    @GitRepositoryQuarkusApplication(repo = REPO, mavenArgs = DEFAULT_ARGS + UBER)
    static final RestService app = new RestService();

    @GitRepositoryQuarkusApplication(repo = REPO, artifact = "todo-backend-1.0-SNAPSHOT-runner.jar", mavenArgs = DEFAULT_ARGS
            + UBER)
    static final RestService explicit = new RestService();

    private static final String NO_SUFFIX = "-Dquarkus.package.add-runner-suffix=false";

    @GitRepositoryQuarkusApplication(repo = REPO, artifact = "todo-backend-1.0-SNAPSHOT.jar", mavenArgs = DEFAULT_ARGS + UBER
            + NO_SUFFIX)
    static final RestService unsuffixed = new RestService();

    @Test
    public void verify() {
        app.given().get().then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void verifyExplicitArtifact() {
        explicit.given().get().then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void verifyNoSuffix() {
        unsuffixed.given().get().then().statusCode(HttpStatus.SC_OK);
    }
}
