package io.quarkus.test.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.FileLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.ProcessBuilderProvider;

public class QuarkusHelmFileClient {
    public static final String COMMAND_LOG_FILE = "quarkus-helmfile-command.out";
    private static final PropertyLookup COMMAND = new PropertyLookup("ts.quarkus.helmfile.cmd", "helmfile");
    private static final Path TARGET = Paths.get("target");

    private final ScenarioContext context;

    public QuarkusHelmFileClient(ScenarioContext ctx) {
        this.context = ctx;
    }

    public QuarkusHelmFileClient.Result sync(Path helmFileDirectory, String helmfileName) {
        return run(helmFileDirectory, "-f", helmfileName, "sync");
    }

    public QuarkusHelmFileClient.Result uninstall(Path helmFileDirectory, String helmfileName) {
        return run(helmFileDirectory, "-f", helmfileName, "destroy");
    }

    public QuarkusHelmFileClient.Result run(Path helmFileDirectory, String... args) {
        QuarkusHelmFileClient.Result result = new QuarkusHelmFileClient.Result();
        File output = TARGET.resolve(COMMAND_LOG_FILE).toFile();

        try (FileLoggingHandler loggingHandler = new FileLoggingHandler(output)) {
            loggingHandler.startWatching();
            List<String> cmd = buildCmd(args);
            result.commandExecuted = cmd.stream().collect(Collectors.joining(" "));
            Log.info(result.commandExecuted);
            Process process = runCli(helmFileDirectory, output, cmd);
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            Log.warn("Failed to run Quarkus Helm command. Caused by: " + e.getMessage());
            result.exitCode = 1;
        }

        result.output = FileUtils.loadFile(output).trim();
        FileUtils.deleteFileContent(output);
        return result;
    }

    private List<String> buildCmd(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(COMMAND.get().split(" ")));
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    private Process runCli(Path workingDirectory, File logOutput, List<String> cmd) {
        try {
            return ProcessBuilderProvider.command(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(logOutput)
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public class Result {

        private static final int EXIT_SUCCESS = 0;

        protected int exitCode;
        protected String output;
        protected String commandExecuted;

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isSuccessful() {
            return EXIT_SUCCESS == exitCode;
        }

        public String getCommandExecuted() {
            return commandExecuted;
        }
    }
}
