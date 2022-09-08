package io.quarkus.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@Tag("quarkus-helm")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus Helm not supported")
public class QuarkusHelmClientIT {

    @Inject
    static QuarkusHelmClient helmClient;

    @Test
    public void verifyHelmVersionExist() {
        QuarkusHelmClient.Result helmCmdResult = helmClient.run("version");
        Assertions.assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));
        Assertions.assertFalse(
                helmCmdResult.getOutput().isEmpty(),
                "Unexpected helm version");
    }
}
