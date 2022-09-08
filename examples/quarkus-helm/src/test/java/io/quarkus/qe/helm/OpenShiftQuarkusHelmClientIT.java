package io.quarkus.qe.helm;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@Tag("quarkus-helm")
@OpenShiftScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
//TODO https://github.com/quarkiverse/quarkus-helm/issues/48
@Disabled
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
