package io.quarkus.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import io.quarkus.test.logging.Log;

public class Command {

    private final String description;
    private final List<String> command;

    private BiConsumer<String, InputStream> outputConsumer = consoleOutput();
    private String directory = ".";

    public Command(String... command) {
        this(Arrays.asList(command));
    }

    public Command(List<String> command) {
        this.description = descriptionOfProgram(command.get(0));
        this.command = command;
    }

    public Command outputToConsole() {
        outputConsumer = consoleOutput();
        return this;
    }

    public Command outputToLines(List<String> output) {
        outputConsumer = linesOutput(output);
        return this;
    }

    public Command onDirectory(Path path) {
        directory = path.toString();
        return this;
    }

    private static String descriptionOfProgram(String program) {
        if (program.contains(File.separator)) {
            return program.substring(program.lastIndexOf(File.separator) + 1);
        }
        return program;
    }

    public void runAndWait(Duration waitAtLeast) throws IOException, InterruptedException {
        runAndWait();
        Thread.sleep(waitAtLeast.toMillis());
    }

    public void runAndWait() throws IOException, InterruptedException {
        Log.info("Running command: %s", String.join(" ", command));
        Process process = ProcessBuilderProvider.command(command).redirectErrorStream(true)
                .directory(new File(directory).getAbsoluteFile()).start();

        new Thread(() -> outputConsumer.accept(description, process.getInputStream()),
                "stdout consumer for command " + description).start();

        int result = process.waitFor();
        if (result != 0) {
            throw new RuntimeException(description + " failed (executed " + command + ", return code " + result + ")");
        }
    }

    private static BiConsumer<String, InputStream> linesOutput(List<String> lines) {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.info("%s: %s", description, line);
                    lines.add(line);
                }
            } catch (IOException ignored) {
            }
        };
    }

    private static BiConsumer<String, InputStream> consoleOutput() {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.info("%s: %s", description, line);
                }
            } catch (IOException ignored) {
            }
        };
    }
}
