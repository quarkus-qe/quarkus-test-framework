package io.quarkus.ts.openshift.sqldb;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MySqlDatabaseIT extends AbstractSqlDatabaseIT {

    static final String MYSQL_USER = "user";
    static final String MYSQL_PASSWORD = "user";
    static final String MYSQL_DATABASE = "mydb";
    static final int MYSQL_PORT = 3306;

    @Container(image = "mysql/mysql-server:8.0.22", port = MYSQL_PORT, expectedLog = "port: 3306  MySQL Community Server")
    static DefaultService database = new DefaultService()
            .withProperty("MYSQL_USER", MYSQL_USER)
            .withProperty("MYSQL_PASSWORD", MYSQL_PASSWORD)
            .withProperty("MYSQL_DATABASE", MYSQL_DATABASE);

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.username", MYSQL_USER)
            .withProperty("quarkus.datasource.password", MYSQL_PASSWORD)
            .withProperty("quarkus.datasource.jdbc.url",
                    () -> database.getHost().replace("http", "jdbc:mysql") + ":" + database.getPort() + "/" + MYSQL_DATABASE);

    @Override
    protected RestService getApp() {
        return app;
    }
}
