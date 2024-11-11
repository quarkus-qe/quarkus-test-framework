package io.quarkus.qe.database.mysql;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

/**
 * Running Quarkus on DEV mode will spin up a Database instance automatically.
 */
@QuarkusScenario
public class DevModeMySqlDevServicesDatabaseIT extends AbstractSqlDatabaseIT {

    @DevModeQuarkusApplication
    static final RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }

    @Test
    public void verifyLogsToAssertDevMode() {
        app.logs().assertContains("Profile DevModeMySqlDevServicesDatabaseIT activated. Live Coding activated");
    }
}
