package io.quarkus.qe.database.mysql;

import static io.quarkus.qe.database.mysql.DevModeMySqlDatabaseIT.MYSQL_PORT;

import io.quarkus.test.bootstrap.MySqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MySqlDatabaseIT extends AbstractSqlDatabaseIT {

    @Container(image = "docker.io/mysql:8.0.27", port = MYSQL_PORT, expectedLog = "port: 3306  MySQL Community Server")
    static MySqlService database = new MySqlService();

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
