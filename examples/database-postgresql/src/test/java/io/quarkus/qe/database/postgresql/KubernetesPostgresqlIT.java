/*  File: KubernetesPostgresqlIT.java
    Author: Georgii Troitskii (xtroit00)
    Date: 9.5.2024
*/

package io.quarkus.qe.database.postgresql;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@KubernetesScenario
public class KubernetesPostgresqlIT extends AbstractSqlDatabaseIT {

    private static final int POSTGRESQL_PORT = 5432;

    @Container(image = "${postgresql.image}", port = POSTGRESQL_PORT, expectedLog = "is ready")
    static final PostgresqlService database = new PostgresqlService()
            .withProperty("PGDATA", "/tmp/psql");

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @Override
    protected RestService getApp() {
        return app;
    }
}
