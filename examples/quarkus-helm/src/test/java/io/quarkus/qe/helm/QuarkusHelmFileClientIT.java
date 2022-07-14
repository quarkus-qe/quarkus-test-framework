package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmFileClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@Tag("quarkus-helm")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus Helm not supported")
// TODO https://github.com/quarkiverse/quarkus-helm/issues/29
@DisabledOnQuarkusSnapshot(reason = "unsupported quarkus-helm/dekorate version")
public class QuarkusHelmFileClientIT {

    private final static String EXPECTED_HELMFILE_VERSION_REGEXP = ".*[v]{1}\\d{1,2}\\.\\d{1,3}\\.\\d{1,3}.*";
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
        assertTrue(
                helmCmdResult.getOutput().matches(EXPECTED_HELMFILE_VERSION_REGEXP),
                "Unexpected helm version");
    }
}
