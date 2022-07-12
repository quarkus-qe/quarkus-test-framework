package io.quarkus.qe.helm;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;

@Tag("quarkus-helm")
@OpenShiftScenario
// TODO https://github.com/quarkiverse/quarkus-helm/issues/29
@DisabledOnQuarkusSnapshot(reason = "unsupported quarkus-helm/dekorate version")
public class OpenShiftQuarkusHelmClientIT extends CommonScenarios {

    @Inject
    static QuarkusHelmClient helmClient;

    @Override
    protected QuarkusHelmClient getHelmClient() {
        return helmClient;
    }
}
