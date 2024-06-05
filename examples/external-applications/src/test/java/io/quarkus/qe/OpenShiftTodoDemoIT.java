package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@DisabledOnNative(reason = "Native + s2i not supported")
@OpenShiftScenario
public class OpenShiftTodoDemoIT extends TodoDemoIT {
}
