package io.quarkus.qe;

import io.quarkus.test.annotations.DisabledIfNotContainerRegistry;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
@DisabledIfNotContainerRegistry
public class OpenShiftUsingContainerRegistrySecurityResourceIT extends SecurityResourceIT {
}
