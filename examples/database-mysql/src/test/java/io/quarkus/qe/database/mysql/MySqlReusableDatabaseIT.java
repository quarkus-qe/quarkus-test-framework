package io.quarkus.qe.database.mysql;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MySqlReusableDatabaseIT extends AbstractMysqlReusableInstance {

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl);

    @Override
    protected RestService getApp() {
        return app;
    }

    @Test
    public void verifyContainerIsReused() {
        // Ignored if is the first Mysql instance
        // This test works in conjunction with MySqlDatabaseIT.verifyContainerIsReused
        IsContainerReused();
    }
}
