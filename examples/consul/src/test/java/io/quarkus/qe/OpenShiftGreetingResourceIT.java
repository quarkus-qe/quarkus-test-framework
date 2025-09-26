package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftScenario;

@Disabled("https://github.com/quarkus-qe/quarkus-test-framework/issues/1708")
@OpenShiftScenario
public class OpenShiftGreetingResourceIT extends GreetingResourceIT {
}
