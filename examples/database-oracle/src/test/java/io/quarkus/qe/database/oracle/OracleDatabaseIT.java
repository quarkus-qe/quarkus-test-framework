package io.quarkus.qe.database.oracle;

import static io.quarkus.qe.database.oracle.DevModeOracleDatabaseIT.ORACLE_PORT;

import io.quarkus.test.bootstrap.OracleService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class OracleDatabaseIT extends AbstractSqlDatabaseIT {

    @Container(image = "${oracle.image}", port = ORACLE_PORT, expectedLog = "DATABASE IS READY TO USE!")
    static final OracleService database = new OracleService();

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
