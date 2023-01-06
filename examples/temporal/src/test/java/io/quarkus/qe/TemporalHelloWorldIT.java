package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@DisabledOnNative(reason = "Not supported by Temporal")
public class TemporalHelloWorldIT {

    private static final String TEMPORAL_DB_TYPE = "postgresql";
    private static final String DEFAULT_POSTGRES_CONFIG = "config/dynamicconfig/development-sql.yaml";
    private static final String CONTAINER_FILE_DEPLOYMENT_PATH = "/etc/temporal/config/dynamicconfig";
    private static final String DEPLOYMENT_CASS_FILE = "development-cass.yaml";
    private static final String DOCKER_FILE = "docker.yaml";
    private static final String DEPLOYMENT_SQL_FILE = "development-sql.yaml";
    private static final String CONTAINER_RESOURCE = "resource_with_destination";
    private static final String NAMESPACE = "default";
    static final int POSTGRESQL_PORT = 5432;
    static final int TEMPORAL_PORT = 7233;

    protected static final String POSTGRES_USER = "temporal";
    protected static final String POSTGRES_PWD = "temporal";

    @Container(image = "docker.io/library/postgres:latest", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address", networkAlias = TEMPORAL_DB_TYPE, networkId = TEMPORAL_DB_TYPE)
    static PostgresqlService postgres = new PostgresqlService()
            .with(POSTGRES_USER, POSTGRES_PWD, "temporal");

    @Container(image = "temporalio/auto-setup:1.19.0", port = TEMPORAL_PORT, expectedLog = "Temporal server started", networkId = TEMPORAL_DB_TYPE)
    static DefaultService temporal = new DefaultService()
            .withProperty("POSTGRES_USER", () -> postgres.getUser())
            .withProperty("POSTGRES_PWD", () -> postgres.getPassword())
            .withProperty("DB", TEMPORAL_DB_TYPE)
            .withProperty("DB_PORT", "" + POSTGRESQL_PORT)
            .withProperty("POSTGRES_SEEDS", TEMPORAL_DB_TYPE)
            .withProperty("DYNAMIC_CONFIG_FILE_PATH", DEFAULT_POSTGRES_CONFIG)
            .withProperty("DEFAULT_NAMESPACE", NAMESPACE)
            .withProperty("DEPLOYMENT_CASS",
                    String.format("%s::%s|%s", CONTAINER_RESOURCE, CONTAINER_FILE_DEPLOYMENT_PATH, DEPLOYMENT_CASS_FILE))
            .withProperty("DOCKER_YAML",
                    String.format("%s::%s|%s", CONTAINER_RESOURCE, CONTAINER_FILE_DEPLOYMENT_PATH, DOCKER_FILE))
            .withProperty("DEPLOYMENT_SQL",
                    String.format("%s::%s|%s", CONTAINER_RESOURCE, CONTAINER_FILE_DEPLOYMENT_PATH, DEPLOYMENT_SQL_FILE))
            .onPostStart(service -> wait(Duration.ofSeconds(10)));

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("temporal.host", () -> temporal.getURI().getHost())
            .withProperty("temporal.port", () -> "" + temporal.getURI().getPort())
            .withProperty("temporal.namespace", NAMESPACE);

    @Test
    public void temporalHelloWorld() {
        app.given().get("/workflow/hello/pablo").then().statusCode(HttpStatus.SC_OK).and().body(is("Hello pablo!"));
    }

    //TODO https://github.com/temporalio/temporal/issues/1336
    static final void wait(Duration amount) {
        try {
            Thread.sleep(amount.toMillis());
        } catch (InterruptedException e) {

        }
    }
}
