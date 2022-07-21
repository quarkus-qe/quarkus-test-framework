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

    private final static String EXPECTED_HELM_VERSION_REGEXP = ".*[v]{1}\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}.*";

    @Inject
    static QuarkusHelmClient helmClient;

    @Test
    public void verifyHelmVersionExist() {
        QuarkusHelmClient.Result helmCmdResult = helmClient.run("version");
        Assertions.assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));
        Assertions.assertTrue(
                helmCmdResult.getOutput().matches(EXPECTED_HELM_VERSION_REGEXP),
                "Unexpected helm version");
    }
}
