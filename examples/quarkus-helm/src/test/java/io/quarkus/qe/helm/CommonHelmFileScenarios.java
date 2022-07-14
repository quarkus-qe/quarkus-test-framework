package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmFileClient;

public abstract class CommonHelmFileScenarios {

    protected abstract QuarkusHelmFileClient getHelmFileClient();

    private static Path helmfilesFolder;

    @BeforeAll
    public static void tearUp() {
        helmfilesFolder = Paths.get("src", "test", "resources", "helmfiles").toAbsolutePath();
    }

    @Test
    public void verifyHelmfileSync() {
        QuarkusHelmFileClient.Result helmCmdResult = getHelmFileClient().sync(helmfilesFolder, "helmfile.yaml");
        assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));

        getHelmFileClient().uninstall(helmfilesFolder, "helmfile.yaml");
    }
}
