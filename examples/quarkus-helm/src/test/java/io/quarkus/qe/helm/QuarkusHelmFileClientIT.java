package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkus.test.bootstrap.QuarkusHelmFileClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@QuarkusScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
@EnabledIf(value = "io.quarkus.test.bootstrap.HelmUtils#isHelmFileInstalled", disabledReason = "Helmfile needs to be locally installed")
public class QuarkusHelmFileClientIT {

    private static Path helmfilesFolder;

    @Inject
    static QuarkusHelmFileClient helmFileClient;

    @BeforeAll
    public static void tearUp() {
        helmfilesFolder = Paths.get("src", "test", "resources", "helmfiles").toAbsolutePath();
    }

    @Test
    public void verifyHelmFileInjection() {
        QuarkusHelmFileClient.Result helmCmdResult = helmFileClient.run(helmfilesFolder.toAbsolutePath(), "-version");
        assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));
        assertFalse(
                helmCmdResult.getOutput().isEmpty(),
                "Unexpected helm version");
    }
}
