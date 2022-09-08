package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.QuarkusHelmFileClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@Tag("quarkus-helm")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus Helm not supported")
@DisabledOnOs(OS.WINDOWS)
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
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
