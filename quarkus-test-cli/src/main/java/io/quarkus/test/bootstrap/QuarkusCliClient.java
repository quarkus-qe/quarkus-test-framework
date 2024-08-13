package io.quarkus.test.bootstrap;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.FileLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.quarkus.CliDevModeLocalhostQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.CliDevModeVersionLessQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.ProcessBuilderProvider;

public class QuarkusCliClient {

    public static final String COMMAND_LOG_FILE = "quarkus-cli-command.out";
    public static final String DEV_MODE_LOG_FILE = "quarkus-cli-dev.out";

    private static final String QUARKUS_VERSION_PROPERTY_NAME = "quarkus.version";
    private static final String QUARKUS_UPSTREAM_VERSION = "999-SNAPSHOT";
    private static final String BUILD = "build";
    private static final PropertyLookup COMMAND = new PropertyLookup("ts.quarkus.cli.cmd", "quarkus");
    private static final Path TARGET = Paths.get("target");

    private final ScenarioContext context;

    public QuarkusCliClient(ScenarioContext context) {
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
        if (isUpstream()) {
            args.add("-D" + QUARKUS_VERSION_PROPERTY_NAME + "=" + QuarkusProperties.getVersion());
        }
        args.addAll(Arrays.asList(extraArgs));
        return runCliAndWait(serviceFolder, args.toArray(new String[args.size()]));
    }

    public Result buildApplicationOnNative(Path serviceFolder, String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add(BUILD);
        args.add("--native");
        if (isUpstream()) {
            args.add("-D" + QUARKUS_VERSION_PROPERTY_NAME + "=" + QuarkusProperties.getVersion());
        }
        args.addAll(Arrays.asList(extraArgs));
        return runCliAndWait(serviceFolder, args.toArray(new String[args.size()]));
    }

    public Process runOnDev(Path servicePath, File logOutput, Map<String, String> arguments) {
        List<String> cmd = new ArrayList<>();
        cmd.add("dev");
        cmd.addAll(arguments.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .toList());
        return runCli(servicePath, logOutput, cmd.toArray(new String[cmd.size()]));
    }

    public QuarkusCliRestService createApplicationAt(String name, String targetFolderName) {
        Objects.requireNonNull(targetFolderName);
        return createApplication(name, CreateApplicationRequest.defaults(), targetFolderName);
    }

    public QuarkusCliRestService createApplication(String name) {
        return createApplication(name, CreateApplicationRequest.defaults());
    }

    public QuarkusCliRestService createApplication(String name, CreateApplicationRequest request) {
        return createApplication(name, request, null);
    }

    public QuarkusCliRestService createApplication(String name, CreateApplicationRequest request, String targetFolderName) {
        Path serviceFolder = isNotEmpty(targetFolderName) ? TARGET.resolve(targetFolderName).resolve(name) : null;
        QuarkusCliRestService service = new QuarkusCliRestService(this, serviceFolder);
        ServiceContext serviceContext = service.register(name, context);

        service.init(request.managedResourceCreator.initBuilder(serviceContext, this));

        // We need the service folder to be emptied before generating the project
        FileUtils.deletePath(serviceContext.getServiceFolder());

        // Generate project
        List<String> args = new ArrayList<>(Arrays.asList("create", "app", name));
        // Platform Bom
        if (isNotEmpty(request.platformBom)) {
            args.add("--platform-bom=" + request.platformBom);
        }
        // Stream
        if (isNotEmpty(request.stream)) {
            args.add("--stream=" + request.stream);
        }
        // Extensions
        if (request.extensions != null && request.extensions.length > 0) {
            args.add("-x=" + Stream.of(request.extensions).collect(Collectors.joining(",")));
        }
        // Extra args
        if (request.extraArgs != null && request.extraArgs.length > 0) {
            args.addAll(Arrays.asList(request.extraArgs));
        }

        Result result = runCliAndWait(serviceContext.getServiceFolder().getParent(), args.toArray(new String[args.size()]));
        assertTrue(result.isSuccessful(), "The application was not created. Output: " + result.getOutput());

        return service;
    }

    public QuarkusCliRestService createApplicationFromExistingSources(String name, String targetFolderName, Path sourcesDir) {
        return createApplicationFromExistingSources(name, targetFolderName, sourcesDir,
                ((serviceContext,
                        quarkusCliClient) -> managedResCreator -> new CliDevModeVersionLessQuarkusApplicationManagedResource(
                                serviceContext, quarkusCliClient)));
    }

    public QuarkusCliRestService createApplicationFromExistingSources(String name, String targetFolderName, Path sourcesDir,
            ManagedResourceCreator managedResourceCreator) {
        Path serviceFolder = isNotEmpty(targetFolderName) ? TARGET.resolve(targetFolderName).resolve(name) : null;
        QuarkusCliRestService service = new QuarkusCliRestService(this, serviceFolder);
        ServiceContext serviceContext = service.register(name, context);

        service.init(managedResourceCreator.initBuilder(serviceContext, this));

        // We need the service folder to be emptied before generating the project
        FileUtils.deletePath(serviceContext.getServiceFolder());

        FileUtils.copyDirectoryTo(sourcesDir, serviceContext.getServiceFolder());

        return service;
    }

    public Result updateApplication(UpdateApplicationRequest request, Path serviceFolder) {
        List<String> args = new ArrayList<>(List.of("update"));

        // stream
        if (isNotEmpty(request.stream)) {
            args.add("--stream=" + request.stream);
        }

        // platform-version
        if (isNotEmpty(request.platformVersion)) {
            args.add("--platform-version=" + request.platformVersion);
        }

        Result result = runCliAndWait(serviceFolder, args.toArray(new String[0]));
        assertTrue(result.isSuccessful(), "The application was not updated. Output: " + result.getOutput());
        return result;
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public QuarkusCliDefaultService createExtension(String name) {
        return createExtension(name, CreateExtensionRequest.defaults());
    }

    public QuarkusCliDefaultService createExtension(String name, CreateExtensionRequest request) {
        QuarkusCliDefaultService service = new QuarkusCliDefaultService(this);
        ServiceContext serviceContext = service.register("quarkus-" + name, context);

        // We need the service folder parent to exist for cli log file
        FileUtils.createDirectory(serviceContext.getServiceFolder().getParent());

        // Generate project
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("create", "extension", name));
        // Platform Bom
        if (isNotEmpty(request.platformBom)) {
            args.add("--platform-bom=" + request.platformBom);
        }
        // Stream
        if (isNotEmpty(request.stream)) {
            args.add("--stream=" + request.stream);
        }
        // Extra args
        if (request.extraArgs != null && request.extraArgs.length > 0) {
            args.addAll(Arrays.asList(request.extraArgs));
        }

        Result result = runCliAndWait(serviceContext.getServiceFolder().getParent(), args.toArray(new String[args.size()]));
        assertTrue(result.isSuccessful(), "The extension was not created. Output: " + result.getOutput());

        return service;
    }

    private Result runCliAndWait(String... args) {
        return runCliAndWait(TARGET, args);
    }

    private Result runCliAndWait(Path workingDirectory, String... args) {
        Result result = new Result();
        File output = workingDirectory.resolve(COMMAND_LOG_FILE).toFile();

        try (FileLoggingHandler loggingHandler = new FileLoggingHandler(output)) {
            loggingHandler.startWatching();
            Process process = runCli(workingDirectory, output, args);
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            Log.warn("Failed to run Quarkus CLI command. Caused by: " + e.getMessage());
            result.exitCode = 1;
        }

        result.output = FileUtils.loadFile(output).trim();
        FileUtils.deleteFileContent(output);
        return result;
    }

    private Process runCli(Path workingDirectory, File logOutput, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(COMMAND.get().split(" ")));
        cmd.addAll(Arrays.asList(args));

        if (QuarkusProperties.disableBuildAnalytics()) {
            cmd.add(format("-D%s=%s", QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, Boolean.TRUE));
        }

        Log.info(String.join(" ", cmd));
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

    private static boolean isUpstream() {
        return isUpstream(QuarkusProperties.getVersion());
    }

    private static boolean isUpstream(String version) {
        return version.contains(QUARKUS_UPSTREAM_VERSION);
    }

    private static String getFixedStreamVersion() {
        var rawVersion = QuarkusProperties.getVersion();
        if (isUpstream(rawVersion)) {
            throw new IllegalStateException("Cannot set fixed stream version for '%s' as it doesn't exist" + rawVersion);
        }

        String[] version = rawVersion.split(Pattern.quote("."));
        return String.format("%s.%s", version[0], version[1]);
    }

    private static String getCurrentPlatformBom() {
        return QuarkusProperties.PLATFORM_GROUP_ID.get() + "::" + QuarkusProperties.getVersion();
    }

    public Result listExtensions(String... extraArgs) {
        return listExtensions(ListExtensionRequest.defaults(), extraArgs);
    }

    public Result listExtensions(ListExtensionRequest request, String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add("extension");
        args.add("list");
        args.addAll(Arrays.asList(extraArgs));
        if (request.stream() != null) {
            args.addAll(Arrays.asList("--stream", request.stream()));
        }
        var result = run(args.toArray(String[]::new));
        assertTrue(result.isSuccessful(), "Extensions list command didn't work. Output: " + result.getOutput());
        return result;
    }

    public static class CreateApplicationRequest {
        private String platformBom;
        private String stream;
        private String[] extensions;
        private String[] extraArgs;
        private ManagedResourceCreator managedResourceCreator = (serviceContext,
                quarkusCliClient) -> managedResourceBuilder -> new CliDevModeLocalhostQuarkusApplicationManagedResource(
                        serviceContext, quarkusCliClient);

        public CreateApplicationRequest withPlatformBom(String platformBom) {
            this.platformBom = platformBom;
            return this;
        }

        public CreateApplicationRequest withCurrentPlatformBom() {
            return withPlatformBom(getCurrentPlatformBom());
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

        public CreateApplicationRequest withManagedResourceCreator(ManagedResourceCreator managedResourceCreator) {
            this.managedResourceCreator = managedResourceCreator;
            return this;
        }

        public static CreateApplicationRequest defaults() {
            if (isUpstream()) {
                // set platform due to https://github.com/quarkusio/quarkus/issues/40951#issuecomment-2147399201
                return new CreateApplicationRequest().withCurrentPlatformBom();
            }
            // set fixed stream because if tested stream is not the latest stream, we would create app with wrong version
            return new CreateApplicationRequest().withStream(getFixedStreamVersion());
        }
    }

    public interface ManagedResourceCreator {
        ManagedResourceBuilder initBuilder(ServiceContext serviceContext, QuarkusCliClient quarkusCliClient);
    }

    public static class UpdateApplicationRequest {
        private String stream;
        private String platformVersion;

        public UpdateApplicationRequest withStream(String stream) {
            this.stream = stream;
            return this;
        }

        public UpdateApplicationRequest withPlatformVersion(String platformVersion) {
            this.platformVersion = platformVersion;
            return this;
        }

        public static UpdateApplicationRequest defaultUpdate() {
            return new UpdateApplicationRequest();
        }
    }

    public static class CreateExtensionRequest {
        private String platformBom;
        private String stream;
        private String[] extraArgs;

        public CreateExtensionRequest withCurrentPlatformBom() {
            return withPlatformBom(getCurrentPlatformBom());
        }

        public CreateExtensionRequest withPlatformBom(String platformBom) {
            this.platformBom = platformBom;
            return this;
        }

        public CreateExtensionRequest withStream(String stream) {
            this.stream = stream;
            return this;
        }

        public CreateExtensionRequest withExtraArgs(String... extraArgs) {
            this.extraArgs = extraArgs;
            return this;
        }

        public static CreateExtensionRequest defaults() {
            if (isUpstream()) {
                return new CreateExtensionRequest();
            }
            // set fixed stream because if tested stream is not the latest stream, we would create app with wrong version
            return new CreateExtensionRequest().withStream(getFixedStreamVersion());
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

    public record ListExtensionRequest(String stream) {
        public static ListExtensionRequest defaults() {
            return new ListExtensionRequest(isUpstream() ? null : getFixedStreamVersion());
        }

        public static ListExtensionRequest withSetStream() {
            return new ListExtensionRequest(isUpstream() ? QUARKUS_UPSTREAM_VERSION : getFixedStreamVersion());
        }
    }
}
