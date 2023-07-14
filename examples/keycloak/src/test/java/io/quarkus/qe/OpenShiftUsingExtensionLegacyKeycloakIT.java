package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
@DisabledOnQuarkusVersion(version = "3.2.0.Final", reason = "https://github.com/quarkusio/quarkus/issues/34276")
public class OpenShiftUsingExtensionLegacyKeycloakIT extends LegacyKeycloakIT {
}
