package io.quarkus.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

public class Command {

    private static final Logger LOGGER = Logger.getLogger(Command.class);

    private final String description;
    private final List<String> command;

    private BiConsumer<String, InputStream> outputConsumer = consoleOutput();

    public Command(String... command) {
        this(Arrays.asList(command));
    }

    public Command(List<String> command) {
        this.description = descriptionOfProgram(command.get(0));
        this.command = command;
    }

    public Command outputToFile(File file) {
        outputConsumer = fileOutput(file);
        return this;
    }

    public Command outputToConsole() {
        outputConsumer = consoleOutput();
        return this;
    }

    public Command outputToList(List<String> list) {
        outputConsumer = listOutput(list);
        return this;
    }

    private static String descriptionOfProgram(String program) {
        if (program.contains(File.separator)) {
            return program.substring(program.lastIndexOf(File.separator) + 1);
        }
        return program;
    }

    public void runAndWait() throws IOException, InterruptedException {
        LOGGER.infof("Running command: %s", String.join(" ", command));
        Process process = new ProcessBuilder().redirectErrorStream(true).command(command)
                .directory(new File(".").getAbsoluteFile()).start();

        new Thread(() -> outputConsumer.accept(description, process.getInputStream()),
                "stdout consumer for command " + description).start();

        int result = process.waitFor();
        if (result != 0) {
            throw new RuntimeException(description + " failed (executed " + command + ", return code " + result + ")");
        }
    }

    private static final BiConsumer<String, InputStream> fileOutput(File targetFile) {
        return (description, is) -> {
            try (OutputStream outStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException ignored) {
            }
        };
    }

    private static final BiConsumer<String, InputStream> consoleOutput() {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.infof("%s: %s", description, line);
                }
            } catch (IOException ignored) {
            }
        };
    }

    private static final BiConsumer<String, InputStream> listOutput(List<String> list) {
        return (description, is) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
            } catch (IOException ignored) {
            }
        };
    }
}
