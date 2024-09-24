package io.quarkus.qe;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows does not support long file paths")
@QuarkusScenario
public class BareMetalQuickstartUsingDefaultsIT extends QuickstartUsingDefaultsIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", contextDir = "getting-started")
    static final RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }
}
