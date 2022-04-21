package io.quarkus.qe.database.postgresql;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftPostgresqlIT extends AbstractSqlDatabaseIT {

    private static final int POSTGRESQL_PORT = 5432;

    @Container(image = "quay.io/quarkusqeteam/postgres:14.0", port = POSTGRESQL_PORT, expectedLog = "is ready")
    static PostgresqlService database = new PostgresqlService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @Override
    protected RestService getApp() {
        return app;
    }
}
