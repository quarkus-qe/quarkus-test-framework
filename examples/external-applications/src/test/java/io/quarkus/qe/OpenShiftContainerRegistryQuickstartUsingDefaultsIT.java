package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;

// TODO: enable when Quarkus QuickStarts migrates to Quarkus 3
@Disabled("Disabled until Quarkus QuickStarts migrates to Quarkus 3")
@DisabledOnQuarkusSnapshot(reason = "999-SNAPSHOT is not available in the Maven repositories in OpenShift")
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
public class OpenShiftContainerRegistryQuickstartUsingDefaultsIT extends QuickstartUsingDefaultsIT {

}
