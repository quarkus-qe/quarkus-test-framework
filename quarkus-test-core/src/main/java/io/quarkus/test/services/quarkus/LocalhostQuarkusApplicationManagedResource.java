package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.QUARKUS_GRPC_SERVER_PORT_PROPERTY;
import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.QUARKUS_HTTP_PORT_PROPERTY;
import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.QUARKUS_HTTP_SSL_PORT_PROPERTY;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.logging.FileServiceLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.ProcessBuilderProvider;
import io.quarkus.test.utils.ProcessUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.quarkus.test.utils.SocketUtils;

public abstract class LocalhostQuarkusApplicationManagedResource extends QuarkusManagedResource {

    private static final String LOG_OUTPUT_FILE = "out.log";
    private static final List<String> PREFIXES_TO_REPLACE = Arrays.asList(RESOURCE_PREFIX, SECRET_PREFIX);

    private final QuarkusApplicationManagedResourceBuilder model;

    private final File logOutputFile;
    private Process process;
    private LoggingHandler loggingHandler;
    private int assignedHttpPort;
    private int assignedHttpsPort;
    private int assignedGrpcPort;

    public LocalhostQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        super(model.getContext());
        this.model = model;
        this.logOutputFile = new File(model.getContext().getServiceFolder().resolve(LOG_OUTPUT_FILE).toString());
    }

    protected abstract List<String> prepareCommand(List<String> systemArguments);

    @Override
    public void start() {
        if (process != null && process.isAlive()) {
            // do nothing
            return;
        }

        try {
            assignPorts();
            List<String> command = prepareCommand(getPropertiesForCommand());
            Log.info("Running command: %s", String.join(" ", command));

            ProcessBuilder pb = ProcessBuilderProvider.command(command)
                    .redirectErrorStream(true)
                    .redirectOutput(getLogOutputFile())
                    .directory(getApplicationFolder().toFile());

            onPreStart(pb);

            process = pb.start();

            loggingHandler = new FileServiceLoggingHandler(model.getContext().getOwner(), logOutputFile);
            loggingHandler.startWatching();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        ProcessUtils.destroy(process);
    }

    @Override
    public URILike getURI(Protocol protocol) {
        if ((protocol == Protocol.HTTPS || protocol == Protocol.WSS) && !model.isSslEnabled()) {
            fail("SSL was not enabled. Use: `@QuarkusApplication(ssl = true)`");
        } else if (protocol == Protocol.GRPC && !model.isGrpcEnabled()) {
            fail("gRPC was not enabled. Use: `@QuarkusApplication(grpc = true)`");
        }
        int port = switch (protocol) {
            case HTTPS, WSS -> assignedHttpsPort;
            case GRPC -> assignedGrpcPort;
            default -> assignedHttpPort;
        };
        if (protocol == Protocol.MANAGEMENT && model.useSeparateManagementInterface()) {
            return createURI(model.useManagementSsl() ? "https" : "http",
                    "localhost",
                    model.getManagementPort());
        }
        return createURI(protocol.getValue(), "localhost", port);
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        if (model.buildPropertiesChanged()) {
            model.build();
        }

        start();
    }

    @Override
    public boolean isRunning() {
        return process != null && process.isAlive() && super.isRunning();
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    protected File getLogOutputFile() {
        return logOutputFile;
    }

    protected Path getApplicationFolder() {
        return model.getContext().getServiceFolder();
    }

    protected void onPreStart(ProcessBuilder pb) {

    }

    private void assignPorts() {
        assignedHttpPort = getOrAssignPortByProperty(QUARKUS_HTTP_PORT_PROPERTY);
        if (model.isSslEnabled()) {
            assignedHttpsPort = getOrAssignPortByProperty(QUARKUS_HTTP_SSL_PORT_PROPERTY);
        }

        if (model.isGrpcEnabled()) {
            if (QuarkusProperties.useSeparateGrpcServer(getContext())) {
                assignedGrpcPort = getOrAssignPortByProperty(QUARKUS_GRPC_SERVER_PORT_PROPERTY);
            } else {
                assignedGrpcPort = assignedHttpPort;
            }
        }
    }

    private int getOrAssignPortByProperty(String property) {
        return model.getContext().getOwner().getProperty(property)
                .filter(StringUtils::isNotEmpty)
                .map(Integer::parseInt)
                .orElseGet(SocketUtils::findAvailablePort);
    }

    private List<String> getPropertiesForCommand() {
        Map<String, String> runtimeProperties = new HashMap<>(model.getContext().getOwner().getProperties());
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, "" + assignedHttpPort);
        if (model.isSslEnabled()) {
            runtimeProperties.putIfAbsent(QUARKUS_HTTP_SSL_PORT_PROPERTY, "" + assignedHttpsPort);
        }

        if (model.isGrpcEnabled()) {
            runtimeProperties.putIfAbsent(QUARKUS_GRPC_SERVER_PORT_PROPERTY, "" + assignedGrpcPort);
        }

        // Collect all properties and if some are JVM option properties (start with -X or -XX) the initial -D is not added
        // as they already contain the prefix for JVM. These properties should be fully in hand of user as some of them
        // can contain multiple `=`. Other properties are transformed to follow the format of `-D<key>=<value>`
        return runtimeProperties.entrySet().stream()
                .map(e -> {
                    if (e.getKey().startsWith("-X") || e.getKey().startsWith("-XX")) {
                        return PropertiesUtils.toJvmSystemProperty(e.getKey(), getComputedValue(e.getValue()));
                    }
                    return PropertiesUtils.toMvnSystemProperty(e.getKey(), getComputedValue(e.getValue()));
                })
                .collect(Collectors.toList());
    }

    private String getComputedValue(String value) {
        for (String prefix : PREFIXES_TO_REPLACE) {
            if (value.startsWith(prefix)) {
                return StringUtils.removeStart(value, prefix);
            }
        }

        return value;
    }
}
