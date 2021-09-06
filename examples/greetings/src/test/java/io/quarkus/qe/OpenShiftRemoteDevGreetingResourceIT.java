package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@OpenShiftScenario
@DisabledOnNative
public class OpenShiftRemoteDevGreetingResourceIT extends RemoteDevGreetingResourceIT {
}
