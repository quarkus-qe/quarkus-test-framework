package io.quarkus.qe;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
@Disabled
//TODO https://github.com/quarkusio/quarkus/issues/24277
public class OpenShiftGreetingResourceIT extends GreetingResourceIT {
}
