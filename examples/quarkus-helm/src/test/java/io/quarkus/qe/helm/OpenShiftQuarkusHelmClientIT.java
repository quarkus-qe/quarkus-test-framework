package io.quarkus.qe.helm;

import jakarta.inject.Inject;

import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@OpenShiftScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
@EnabledIf(value = "io.quarkus.test.bootstrap.HelmUtils#isHelmInstalled", disabledReason = "Helm needs to be locally installed")
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
