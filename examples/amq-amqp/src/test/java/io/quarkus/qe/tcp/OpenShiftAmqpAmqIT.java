package io.quarkus.qe.tcp;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@DisabledOnQuarkusVersion(version = "2\\.1\\..*", reason = "Caused by https://github.com/quarkusio/quarkus/issues/18956")
@DisabledOnQuarkusSnapshot(reason = "Caused by https://github.com/quarkusio/quarkus/issues/18956")
@OpenShiftScenario
public class OpenShiftAmqpAmqIT extends AmqpAmqIT {

}
