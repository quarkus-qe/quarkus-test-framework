package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;
import io.restassured.http.ContentType;

@DisabledOnNative(reason = "This scenario is using uber-jar, so it's incompatible with Native")
@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoDemoIT {
    private static final String REPO = "https://github.com/quarkusio/todo-demo-app.git";
    private static final String COMMIT = "b94a724697efdae23d44b289d4f697a52efea8d2";
    private static final String DEFAULT_ARGS = "-DskipTests=true -Dquarkus.platform.group-id=${QUARKUS_PLATFORM_GROUP-ID} -Dquarkus.platform.version=${QUARKUS_PLATFORM_VERSION} ";
    private static final String UBER = "-Dquarkus.package.jar.type=uber-jar ";
    private static final String NO_SUFFIX = "-Dquarkus.package.jar.add-runner-suffix=false";

    @Container(image = "${postgresql.image}", port = 5432, expectedLog = "is ready")
    static final PostgresqlService database = new PostgresqlService()
            // store data in /tmp/psql as in OpenShift we don't have permissions to /var/lib/postgresql/data
            .withProperty("PGDATA", "/tmp/psql");

    @GitRepositoryQuarkusApplication(repo = REPO, branch = COMMIT, mavenArgs = DEFAULT_ARGS + UBER)
    static final RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @GitRepositoryQuarkusApplication(repo = REPO, branch = COMMIT, artifact = "todo-backend-1.0-SNAPSHOT-runner.jar", mavenArgs = DEFAULT_ARGS
            + UBER)
    static final RestService explicit = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @GitRepositoryQuarkusApplication(repo = REPO, branch = COMMIT, artifact = "todo-backend-1.0-SNAPSHOT.jar", mavenArgs = DEFAULT_ARGS
            + UBER
            + NO_SUFFIX)
    static final RestService unsuffixed = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @Test
    @Order(1)
    public void verify() {
        app.given()
                .contentType(ContentType.JSON)
                .body("{\"title\": \"Use Quarkus\", \"order\": 1, \"url\": \"https://quarkus.io\"}")
                .post("/api")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    @Order(2)
    public void verifyExplicitArtifact() {
        explicit.given()
                .accept(ContentType.JSON)
                .get("/api/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("title", is("Use Quarkus"));
    }

    @Test
    @Order(3)
    public void verifyNoSuffix() {
        unsuffixed.given()
                .pathParam("id", 1)
                .delete("/api/{id}")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
