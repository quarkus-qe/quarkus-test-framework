package io.quarkus.qe;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.KubernetesDeploymentStrategy;
import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.services.QuarkusApplication;

@KubernetesScenario(deployment = KubernetesDeploymentStrategy.UsingKubernetesExtension)
public class KubernetesUsingExtensionPingPongResourceIT {

    @QuarkusApplication
    static final RestService pingpong = new RestService()
            .withProperty("quarkus.kubernetes.service-type", "LoadBalancer");

    @Test
    public void shouldPingPongIsUpAndRunning() {
        pingpong.logs().forQuarkus().installedFeatures().contains("kubernetes");
    }
}
