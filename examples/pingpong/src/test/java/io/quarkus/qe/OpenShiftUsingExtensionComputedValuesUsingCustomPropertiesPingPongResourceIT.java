package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@Disabled("https://github.com/quarkusio/quarkus/issues/31228")
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
public class OpenShiftUsingExtensionComputedValuesUsingCustomPropertiesPingPongResourceIT
        extends ComputedValuesUsingCustomPropertiesPingPongResourceIT {
}
