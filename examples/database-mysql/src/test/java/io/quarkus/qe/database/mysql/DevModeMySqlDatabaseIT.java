package io.quarkus.qe.database.mysql;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.DevModeQuarkusApplication;

/**
 * This test verifies that resources in test can be used in DevMode.
 */
@DisabledOnNative
@QuarkusScenario
public class DevModeMySqlDatabaseIT extends AbstractSqlDatabaseIT {

    static final String MYSQL_USER = "user";
    static final String MYSQL_PASSWORD = "user";
    static final String MYSQL_DATABASE = "mydb";
    static final int MYSQL_PORT = 3306;

    @Container(image = "docker.io/mysql:8.0", port = MYSQL_PORT, expectedLog = "ready for connections")
    static DefaultService database = new DefaultService()
            .withProperty("MYSQL_ROOT_USER", MYSQL_USER)
            .withProperty("MYSQL_ROOT_PASSWORD", MYSQL_PASSWORD)
            .withProperty("MYSQL_USER", MYSQL_USER)
            .withProperty("MYSQL_PASSWORD", MYSQL_PASSWORD)
            .withProperty("MYSQL_DATABASE", MYSQL_DATABASE);

    @DevModeQuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.hibernate-orm.sql-load-script", "import-in-test.sql")
            .withProperty("quarkus.datasource.username", MYSQL_USER)
            .withProperty("quarkus.datasource.password", MYSQL_PASSWORD)
            .withProperty("quarkus.datasource.jdbc.url",
                    () -> database.getURI()
                            .withScheme("jdbc:mysql")
                            .withPath("/" + MYSQL_DATABASE)
                            .toString());

    @Override
    protected RestService getApp() {
        return app;
    }
}
