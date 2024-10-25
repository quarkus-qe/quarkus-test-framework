package io.quarkus.qe;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@DisabledOnNative(reason = "https://github.com/quarkusio/quarkus/issues/44142")
@OpenShiftScenario
public class OpenShiftS2iQuickstartUsingDefaultsIT extends QuickstartUsingDefaultsIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", contextDir = "getting-started")
    static final RestService app = new RestService();

    @Override
    protected RestService getApp() {
        return app;
    }
}
