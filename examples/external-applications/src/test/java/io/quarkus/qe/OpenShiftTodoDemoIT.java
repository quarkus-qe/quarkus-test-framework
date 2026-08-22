package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import org.junit.jupiter.api.Disabled;

@Disabled("Not yet moved to Quarkus 4")
@DisabledOnNative(reason = "Native + s2i not supported")
@OpenShiftScenario
public class OpenShiftTodoDemoIT extends TodoDemoIT {
}
