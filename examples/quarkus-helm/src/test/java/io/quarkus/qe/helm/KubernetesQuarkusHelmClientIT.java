package io.quarkus.qe.helm;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@KubernetesScenario
//TODO OCP user can't create a k8s namespace
@Disabled
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
public class KubernetesQuarkusHelmClientIT extends CommonHelmScenarios {

    @Inject
    static QuarkusHelmClient helmClient;

    @Override
    protected QuarkusHelmClient getHelmClient() {
        return helmClient;
    }

    @Override
    protected String getPlatformName() {
        return "kubernetes";
    }
}
