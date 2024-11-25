package io.quarkus.qe.database.oracle;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.DevModeQuarkusApplication;

/**
 * This test verifies that resources in test can be used in DevMode.
 */
@QuarkusScenario
public class DevModeOracleDatabaseIT extends AbstractSqlDatabaseIT {

    static final String ORACLE_USER = "myuser";
    static final String ORACLE_PASSWORD = "user";
    static final String ORACLE_DATABASE = "mydb";
    static final int ORACLE_PORT = 1521;

    @Container(image = "${oracle.image}", port = ORACLE_PORT, expectedLog = "DATABASE IS READY TO USE!")
    static final DefaultService database = new DefaultService()
            .withProperty("APP_USER", ORACLE_USER)
            .withProperty("APP_USER_PASSWORD", ORACLE_PASSWORD)
            .withProperty("ORACLE_PASSWORD", ORACLE_PASSWORD)
            .withProperty("ORACLE_DATABASE", ORACLE_DATABASE);

    @DevModeQuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.hibernate-orm.sql-load-script", "import-in-test.sql")
            .withProperty("quarkus.datasource.username", ORACLE_USER)
            .withProperty("quarkus.datasource.password", ORACLE_PASSWORD)
            .withProperty("quarkus.datasource.jdbc.url",
                    () -> "jdbc:oracle:thin:@:" + database.getURI().getPort() + "/" + ORACLE_DATABASE);

    @Override
    protected RestService getApp() {
        return app;
    }
}
