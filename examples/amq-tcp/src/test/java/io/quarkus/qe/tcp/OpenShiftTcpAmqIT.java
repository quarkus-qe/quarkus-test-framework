package io.quarkus.qe.tcp;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.scenarios.OpenShiftScenario;

// TODO: enable with next Quarkus Artemis bump
@Disabled("Disabled until Quarkus Artemis using Jakarta is released")
@OpenShiftScenario
public class OpenShiftTcpAmqIT extends TcpAmqIT {

}
