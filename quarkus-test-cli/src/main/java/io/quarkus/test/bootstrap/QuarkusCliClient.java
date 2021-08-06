package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.FileLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.quarkus.CliDevModeLocalhostQuarkusApplicationManagedResource;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.ProcessBuilderProvider;

public class QuarkusCliClient {

    public static final String LOG_FILE = "quarkus-cli.out";

    private static final String BUILD = "build";
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

    public Result buildApplicationOnJvm(Path serviceFolder, String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add(BUILD);
        args.addAll(Arrays.asList(extraArgs));
        return runCliAndWait(serviceFolder, args.toArray(new String[args.size()]));
    }

    public Result buildApplicationOnNative(Path serviceFolder, String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add(BUILD);
        args.add("--native");
        args.addAll(Arrays.asList(extraArgs));
        return runCliAndWait(serviceFolder, args.toArray(new String[args.size()]));
    }

    public Process runOnDev(Path servicePath, Map<String, String> arguments) {
        List<String> cmd = new ArrayList<>();
        cmd.add("dev");
        cmd.addAll(arguments.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList()));
        return runCli(servicePath, cmd.toArray(new String[cmd.size()]));
    }

    public QuarkusCliRestService createApplication(String name) {
        return createApplication(name, CreateApplicationRequest.defaults());
    }

    public QuarkusCliRestService createApplication(String name, CreateApplicationRequest request) {
        QuarkusCliRestService service = new QuarkusCliRestService(this);
        ServiceContext serviceContext = service.register(name, context);

        service.init(s -> new CliDevModeLocalhostQuarkusApplicationManagedResource(serviceContext, this));

        // We need the service folder to be emptied before generating the project
        FileUtils.deletePath(serviceContext.getServiceFolder());

        // Generate project
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("create", "app", name));
        // Platform Bom
        if (StringUtils.isNotEmpty(request.platformBom)) {
            args.add("--platform-bom=" + request.platformBom);
        }
        // Stream
        if (StringUtils.isNotEmpty(request.stream)) {
            args.add("--stream=" + request.stream);
        }
        // Extensions
        if (request.extensions != null && request.extensions.length > 0) {
            args.add("-x=" + Stream.of(request.extensions).collect(Collectors.joining(",")));
        }
        // Extra args
        if (request.extraArgs != null && request.extraArgs.length > 0) {
            Stream.of(request.extraArgs).forEach(args::add);
        }

        Result result = runCliAndWait(serviceContext.getServiceFolder().getParent(), args.toArray(new String[args.size()]));
        assertTrue(result.isSuccessful(), "The application was not created. Output: " + result.getOutput());

        return service;
    }

    private Result runCliAndWait(String... args) {
        return runCliAndWait(TARGET, args);
    }

    private Result runCliAndWait(Path workingDirectory, String... args) {
        Result result = new Result();
        File output = workingDirectory.resolve(LOG_FILE).toFile();

        try (FileLoggingHandler loggingHandler = new FileLoggingHandler(output)) {
            loggingHandler.startWatching();
            Process process = runCli(workingDirectory, args);
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            Log.warn("Failed to run Quarkus CLI command. Caused by: " + e.getMessage());
            result.exitCode = 1;
        }

        result.output = FileUtils.loadFile(output).trim();

        return result;
    }

    private Process runCli(Path workingDirectory, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(COMMAND.get().split(" ")));
        cmd.addAll(Arrays.asList(args));

        Log.info(cmd.stream().collect(Collectors.joining(" ")));
        try {
            return ProcessBuilderProvider.command(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(workingDirectory.resolve(LOG_FILE).toFile())
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class CreateApplicationRequest {
        private String platformBom;
        private String stream;
        private String[] extensions;
        private String[] extraArgs;

        public CreateApplicationRequest withPlatformBom(String platformBom) {
            this.platformBom = platformBom;
            return this;
        }

        public CreateApplicationRequest withStream(String stream) {
            this.stream = stream;
            return this;
        }

        public CreateApplicationRequest withExtensions(String... extensions) {
            this.extensions = extensions;
            return this;
        }

        public CreateApplicationRequest withExtraArgs(String... extraArgs) {
            this.extraArgs = extraArgs;
            return this;
        }

        public static CreateApplicationRequest defaults() {
            return new CreateApplicationRequest();
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
