package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;

@DisabledOnQuarkusSnapshot(reason = "999-SNAPSHOT is not available in the Maven repositories in OpenShift")
@DisabledOnNative(reason = "Native + s2i not supported")
@OpenShiftScenario
public class OpenShiftTodoDemoIT extends TodoDemoIT {
}
