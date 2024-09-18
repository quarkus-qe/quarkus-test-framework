package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
@Disabled("https://github.com/quarkus-qe/quarkus-test-framework/issues/1052")
// todo delete when framework support SSL/TLS on openshift
public class OpenShiftTLSIT extends LocalTLSIT {
}
