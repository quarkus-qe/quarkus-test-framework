package io.quarkus.qe;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
public class OpenShiftContainerRegistryQuickstartUsingDefaultsIT extends QuickstartUsingDefaultsIT {
    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", contextDir = "getting-started", branch = "development")
    static final RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }
}
