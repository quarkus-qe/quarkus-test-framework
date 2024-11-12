package io.quarkus.qe;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@OpenShiftScenario
public class OpenShiftS2iQuickstartUsingDefaultsIT extends QuickstartUsingDefaultsIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", branch = "3.15", contextDir = "getting-started")
    static final RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }
}
