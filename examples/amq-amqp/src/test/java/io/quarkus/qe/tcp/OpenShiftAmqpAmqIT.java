package io.quarkus.qe.tcp;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@DisabledOnQuarkusVersion(version = "2\\.1\\..*", reason = "Caused by https://github.com/quarkusio/quarkus/issues/18956")
@DisabledOnQuarkusVersion(version = "999-SNAPSHOT", reason = "Caused by https://github.com/quarkusio/quarkus/issues/18956")
@OpenShiftScenario
public class OpenShiftAmqpAmqIT extends AmqpAmqIT {

}
