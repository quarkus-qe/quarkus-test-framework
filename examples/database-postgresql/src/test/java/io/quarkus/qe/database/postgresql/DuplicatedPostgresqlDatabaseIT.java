package io.quarkus.qe.database.postgresql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class DuplicatedPostgresqlDatabaseIT {

    private static final int POSTGRESQL_PORT = 5432;

    @Container(image = "${postgresql.image}", port = POSTGRESQL_PORT, expectedLog = "is ready")
    static final PostgresqlService database = new PostgresqlService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    //motivation: https://github.com/quarkus-qe/quarkus-test-framework/issues/641
    @Test
    public void verifyGlobalPropertyContainerDeleteImageOnStop() {
        // If test starts means that the run condition between two postgresql scenarios with the same version and
        // global property 'ts.database.container.delete.image.on.stop=true' has been fixed
        Assertions.assertTrue(true);
    }
}
