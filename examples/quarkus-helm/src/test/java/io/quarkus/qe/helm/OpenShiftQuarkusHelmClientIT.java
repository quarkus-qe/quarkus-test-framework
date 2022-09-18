package io.quarkus.qe.helm;

import javax.inject.Inject;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@OpenShiftScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
public class OpenShiftQuarkusHelmClientIT extends CommonHelmScenarios {

    @Inject
    static QuarkusHelmClient helmClient;

    @Override
    protected QuarkusHelmClient getHelmClient() {
        return helmClient;
    }

    @Override
    protected String getPlatformName() {
        return "openshift";
    }
}
