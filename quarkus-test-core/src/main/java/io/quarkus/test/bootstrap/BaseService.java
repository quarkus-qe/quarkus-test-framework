package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.LogsVerifier;
import io.quarkus.test.utils.PropertiesUtils;

public class BaseService<T extends Service> implements Service {

    public static final String SERVICE_STARTUP_TIMEOUT = "startup.timeout";
    public static final Duration SERVICE_STARTUP_TIMEOUT_DEFAULT = Duration.ofMinutes(5);

    private static final String SERVICE_STARTUP_CHECK_POLL_INTERVAL = "startup.check-poll-interval";
    private static final Duration SERVICE_STARTUP_CHECK_POLL_INTERVAL_DEFAULT = Duration.ofSeconds(2);

    private final List<Action> onPreStartActions = new LinkedList<>();
    private final List<Action> onPostStartActions = new LinkedList<>();
    private final Map<String, String> properties = new HashMap<>();
    private final List<Runnable> futureProperties = new LinkedList<>();

    private ManagedResource managedResource;
    private String serviceName;
    private Configuration configuration;
    private ServiceContext context;

    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public T onPreStart(Action action) {
        onPreStartActions.add(action);
        return (T) this;
    }

    public T onPostStart(Action action) {
        onPostStartActions.add(action);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperties(String... propertiesFiles) {
        properties.clear();
        Stream.of(propertiesFiles).map(PropertiesUtils::toMap).forEach(properties::putAll);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    @Override
    public T withProperty(String key, String value) {
        this.properties.put(key, value);
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperty(String key, Supplier<String> value) {
        futureProperties.add(() -> properties.put(key, value.get()));
        return (T) this;
    }

    @Override
    public boolean isRunning() {
        Log.debug(this, "Checking if resource is running");
        if (managedResource == null) {
            Log.debug(this, "Resource is not running");
            return false;
        } else if (managedResource.isFailed()) {
            managedResource.stop();
            fail("Resource failed to start");
        }

        Log.debug(this, "Resource is running");
        return managedResource.isRunning();
    }

    public String getHost() {
        return getHost(Protocol.HTTP);
    }

    public String getHost(Protocol protocol) {
        return managedResource.getHost(protocol);
    }

    public Integer getPort() {
        return getPort(Protocol.HTTP);
    }

    public Integer getPort(Protocol protocol) {
        return managedResource.getPort(protocol);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public List<String> getLogs() {
        return new ArrayList<>(managedResource.logs());
    }

    /**
     * Start the managed resource. If the managed resource is running, it does
     * nothing.
     *
     * @throws RuntimeException when application errors at startup.
     */
    @Override
    public void start() {
        if (isRunning()) {
            return;
        }

        Log.debug(this, "Starting service");

        onPreStartActions.forEach(a -> a.handle(this));
        managedResource.start();
        waitUntilServiceIsStarted();
        onPostStartActions.forEach(a -> a.handle(this));
        Log.info(this, "Service started");
    }

    /**
     * Stop the Quarkus application.
     */
    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }

        Log.debug(this, "Stopping service");
        managedResource.stop();

        Log.info(this, "Service stopped");
    }

    @Override
    public ServiceContext register(String serviceName, ExtensionContext testContext) {
        this.serviceName = serviceName;
        this.configuration = Configuration.load(serviceName);
        this.context = new ServiceContext(this, testContext);
        onPreStart(s -> futureProperties.forEach(Runnable::run));
        testContext.getStore(ExtensionContext.Namespace.create(QuarkusScenarioBootstrap.class)).put(serviceName, this);
        return this.context;
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder) {
        FileUtils.recreateDirectory(context.getServiceFolder());
        managedResource = managedResourceBuilder.build(context);
    }

    public void restart() {
        managedResource.restart();
    }

    @Override
    public LogsVerifier logs() {
        return new LogsVerifier(this);
    }

    protected Path getServiceFolder() {
        return context.getServiceFolder();
    }

    protected <U> U getPropertyFromContext(String key) {
        if (context == null) {
            fail("Service has not been initialized yet. Make sure you invoke this method in the right order.");
        }

        return context.get(key);
    }

    private void waitUntilServiceIsStarted() {
        Duration startupCheckInterval = getConfiguration()
                .getAsDuration(SERVICE_STARTUP_CHECK_POLL_INTERVAL, SERVICE_STARTUP_CHECK_POLL_INTERVAL_DEFAULT);
        Duration startupTimeout = getConfiguration()
                .getAsDuration(SERVICE_STARTUP_TIMEOUT, SERVICE_STARTUP_TIMEOUT_DEFAULT);
        untilIsTrue(this::isRunning, AwaitilitySettings
                .using(startupCheckInterval, startupTimeout)
                .doNotIgnoreExceptions()
                .withService(this)
                .timeoutMessage("Service didn't start in %s minutes", startupTimeout));
    }
}
