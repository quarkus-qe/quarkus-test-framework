package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.quarkus.CliDevModeLocalhostQuarkusApplicationManagedResource;
import io.quarkus.test.utils.FileUtils;

public class QuarkusCliClient {

    public static final String LOG_FILE = "quarkus-cli.out";

    private static final PropertyLookup COMMAND = new PropertyLookup("ts.quarkus.cli.cmd", "quarkus");
    private static final Path TARGET = Paths.get("target");

    private final ExtensionContext context;

    public QuarkusCliClient(ExtensionContext context) {
        this.context = context;
    }

    public Result run(Path servicePath, String... args) {
        return runCliAndWait(servicePath, args);
    }

    public Result run(String... args) {
        return runCliAndWait(args);
    }

    public Result buildApplicationOnJvm(Path servicePath) {
        return runCliAndWait(servicePath, "build");
    }

    public Result buildApplicationOnNative(Path servicePath) {
        return runCliAndWait(servicePath, "build", "--native");
    }

    public Process runOnDev(Path servicePath, Map<String, String> arguments) {
        List<String> cmd = new ArrayList<>();
        cmd.add("dev");
        cmd.addAll(arguments.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList()));
        return runCli(servicePath, cmd.toArray(new String[cmd.size()]));
    }

    public QuarkusCliRestService createApplication(String name, String... extensions) {
        QuarkusCliRestService service = new QuarkusCliRestService(this);
        ServiceContext serviceContext = service.register(name, context);

        service.init(s -> new CliDevModeLocalhostQuarkusApplicationManagedResource(serviceContext, this));

        // We need the service folder to be emptied before generating the project
        FileUtils.deletePath(serviceContext.getServiceFolder());

        // Generate project
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("create", "app", "--artifact-id=" + name));
        args.addAll(Arrays.asList(extensions));
        Result result = runCliAndWait(args.toArray(new String[args.size()]));
        assertTrue(result.isSuccessful(), "The application was not created. Output: " + result.getOutput());

        return service;
    }

    private Result runCliAndWait(String... args) {
        return runCliAndWait(TARGET, args);
    }

    private Result runCliAndWait(Path workingDirectory, String... args) {
        Result result = new Result();

        try {
            Process process = runCli(workingDirectory, args);
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            Log.warn("Failed to run Quarkus CLI command. Caused by: " + e.getMessage());
            result.exitCode = 1;
        }

        result.output = FileUtils.loadFile(workingDirectory.resolve(LOG_FILE).toFile()).trim();

        return result;
    }

    private Process runCli(Path workingDirectory, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(COMMAND.get().split(" ")));
        cmd.addAll(Arrays.asList(args));

        Log.info(cmd.stream().collect(Collectors.joining(" ")));

        try {
            return new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(workingDirectory.resolve(LOG_FILE).toFile())
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Result {

        private static final int EXIT_SUCCESS = 0;

        int exitCode;
        String output;

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isSuccessful() {
            return EXIT_SUCCESS == exitCode;
        }
    }
}
