package io.quarkus.test.bootstrap;

import static io.quarkus.test.configuration.Configuration.Property.CLI_CMD;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.createDisableBuildAnalyticsProperty;
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
    /**
     * Test store property key used to internal (package-private) context propagation of a test service context.
     * This is done because there are many public methods that would have to be changed and still in many scenarios it
     * doesn't make sense to request a service context.
     */
    static final String CLI_SERVICE_CONTEXT_KEY = "io.quarkus.test.bootstrap#cli-service-context";
    private static final String QUARKUS_UPSTREAM_VERSION = "999-SNAPSHOT";
    private static final String BUILD = "build";
    private static final String DEV = "dev";
    private static final PropertyLookup COMMAND = new PropertyLookup(CLI_CMD.getName(), "quarkus");
    private static final Path TARGET = Paths.get("target");

    private final ScenarioContext context;

    public QuarkusCliClient(ScenarioContext context) {
        this.context = context;
        this.context.getTestStore().put(QuarkusCliClient.class.getName(), this);
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
        return runCliAndWait(serviceFolder, args.toArray(new String[0]));
    }

    public Result buildApplicationOnNative(Path serviceFolder, String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add(BUILD);
        args.add("--native");
        PropertyLookup nativeBuilderImageProperty = new PropertyLookup("quarkus.native.builder-image");
        String nativeBuilderImage = nativeBuilderImageProperty.get();
        if (nativeBuilderImage != null && !nativeBuilderImage.isEmpty()) {
            args.add("-D" + nativeBuilderImageProperty.getPropertyKey() + "=" + nativeBuilderImage);
        }
        args.addAll(Arrays.asList(extraArgs));
        return runCliAndWait(serviceFolder, args.toArray(new String[0]));
    }

    public Process runOnDev(Path servicePath, File logOutput, Map<String, String> arguments) {
        List<String> cmd = new ArrayList<>();
        cmd.add("dev");
        cmd.addAll(arguments.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .toList());
        return runCli(servicePath, logOutput, cmd.toArray(new String[0]));
    }

    public QuarkusCliRestService createApplicationAt(String name, String targetFolderName) {
        Objects.requireNonNull(targetFolderName);
        return createApplication(name, getDefaultCreateApplicationRequest(), targetFolderName);
    }

    public QuarkusCliRestService createApplication(String name) {
        return createApplication(name, getDefaultCreateApplicationRequest());
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
            args.add("-x=" + String.join(",", request.extensions));
        }
        // Extra args
        if (request.extraArgs != null && request.extraArgs.length > 0) {
            args.addAll(Arrays.asList(request.extraArgs));
        }

        Result result = runCliAndWait(serviceContext.getServiceFolder().getParent(), args.toArray(new String[0]));
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

        // apply updates
        if (request.applyUpdates) {
            args.add("-y");
        }

        if (!request.additionalArguments.isEmpty()) {
            args.addAll(request.additionalArguments);
        }

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
        return createExtension(name, getDefaultCreateExtensionRequest());
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

        Result result = runCliAndWait(serviceContext.getServiceFolder().getParent(), args.toArray(new String[0]));
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

        // nullable service context, this relies on the fact that CLI services are used in
        // following order: create service, use QuarkusCliClient to run CLI
        // and in the end, we have build-time analytics disabled everywhere except for one module
        // so there is little space for failure; TL;DR; this is not perfect if someone comes with a new order
        ServiceContext serviceContext = (ServiceContext) context.getTestStore().get(CLI_SERVICE_CONTEXT_KEY);
        if (commandSendsAnalytics(cmd) && QuarkusProperties.disableBuildAnalytics(serviceContext)) {
            cmd.add(createDisableBuildAnalyticsProperty());
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

    private static boolean commandSendsAnalytics(List<String> commands) {
        return commands.stream().anyMatch(cmd -> BUILD.equalsIgnoreCase(cmd) || DEV.equalsIgnoreCase(cmd));
    }

    private static boolean isUpstream() {
        return isUpstream(QuarkusProperties.getVersion());
    }

    private static boolean isUpstream(String version) {
        return version.contains(QUARKUS_UPSTREAM_VERSION);
    }

    protected static String getFixedStreamVersion() {
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

    public String getQuarkusVersion() {
        return QuarkusProperties.getVersion();
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
        private boolean applyUpdates = true;
        private List<String> additionalArguments = new ArrayList<>();

        public UpdateApplicationRequest withStream(String stream) {
            this.stream = stream;
            return this;
        }

        public UpdateApplicationRequest withPlatformVersion(String platformVersion) {
            this.platformVersion = platformVersion;
            return this;
        }

        /**
         * Allows to enable or disable Quarkus CLI update command '-y' option.
         * If you need to test the long option ('--yes'), just set 'no' and apply it with additional arguments.
         *
         * @param applyUpdates if updates should be applied without confirmation
         * @return this
         */
        public UpdateApplicationRequest withApplyUpdates(boolean applyUpdates) {
            this.applyUpdates = applyUpdates;
            return this;
        }

        public UpdateApplicationRequest withAdditionalArguments(String... arguments) {
            Objects.requireNonNull(arguments);
            additionalArguments.addAll(Arrays.asList(arguments));
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
                return new CreateExtensionRequest().withCurrentPlatformBom();
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

    ScenarioContext getScenarioContext() {
        return context;
    }

    public CreateApplicationRequest getDefaultCreateApplicationRequest() {
        return CreateApplicationRequest.defaults();
    }

    public CreateExtensionRequest getDefaultCreateExtensionRequest() {
        return CreateExtensionRequest.defaults();
    }
}
