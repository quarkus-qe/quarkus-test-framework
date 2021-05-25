package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.LogsVerifier;
import io.quarkus.test.utils.PropertiesUtils;

public class BaseService<T extends Service> implements Service {

    private static final int SERVICE_WAITER_POLL_EVERY_SECONDS = 2;
    private static final int SERVICE_WAITER_TIMEOUT_MINUTES = 5;

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
        if (isManagedResourceRunning()) {
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
        if (!isManagedResourceRunning()) {
            return;
        }

        Log.debug(this, "Stopping service");
        managedResource.stop();

        Log.info(this, "Service stopped");
    }

    @Override
    public void register(String serviceName) {
        this.serviceName = serviceName;
        this.configuration = Configuration.load(serviceName);
        onPreStart(s -> futureProperties.forEach(Runnable::run));
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder, ServiceContext context) {
        this.context = context;
        Log.info(this, "Initialize service");
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

    protected <U> U getPropertyFromContext(String key) {
        if (context == null) {
            fail("Service has not been initialized yet. Make sure you invoke this method in the right order.");
        }

        return context.get(key);
    }

    private void waitUntilServiceIsStarted() {
        untilIsTrue(this::isManagedResourceRunning, AwaitilitySettings
                .using(Duration.ofSeconds(SERVICE_WAITER_POLL_EVERY_SECONDS),
                        Duration.ofMinutes(SERVICE_WAITER_TIMEOUT_MINUTES))
                .withService(this)
                .timeoutMessage("Service didn't start in %s minutes", SERVICE_WAITER_TIMEOUT_MINUTES));
    }

    private boolean isManagedResourceRunning() {
        Log.debug(this, "Checking if resource is running");
        boolean isRunning = managedResource.isRunning();
        if (isRunning) {
            Log.debug(this, "Resource is running");
        } else {
            Log.debug(this, "Resource is not running");
        }

        return isRunning;
    }
}
