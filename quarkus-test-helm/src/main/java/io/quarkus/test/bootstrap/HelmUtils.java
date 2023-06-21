package io.quarkus.test.bootstrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.utils.Command;

public final class HelmUtils {

    private HelmUtils() {
    }

    public static boolean isHelmInstalled() {
        return validateCommandExecution(QuarkusHelmClient.COMMAND.get(), "version");
    }

    public static boolean isHelmFileInstalled() {
        return validateCommandExecution(QuarkusHelmFileClient.COMMAND.get(), "-version");
    }

    private static boolean validateCommandExecution(String command, String... args) {
        if (OS.WINDOWS.isCurrentOs()) {
            command += ".exe";
        }

        List<String> effectiveCommand = new ArrayList<>();
        effectiveCommand.add(command);
        effectiveCommand.addAll(1, Arrays.asList(args));

        try {
            new Command(effectiveCommand).runAndWait();
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
