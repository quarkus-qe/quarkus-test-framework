package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
@Disabled
// todo delete when https://github.com/quarkusio/quarkus/issues/32225 is fixed
public class OpenShiftTLSIT extends LocalTLSIT {
}
