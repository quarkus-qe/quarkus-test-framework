package io.quarkus.test.bootstrap;

import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

@SuppressWarnings("unchecked")
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
    public T withProperties(String propertiesFile) {
        properties.clear();
        properties.putAll(PropertiesUtils.toMap(propertiesFile));
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
        return managedResource.getHost();
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
        Log.info(this, "Initialize service");
        FileUtils.recreateDirectory(context.getServiceFolder());
        managedResource = managedResourceBuilder.build(context);
    }

    public void restart() {
        managedResource.restart();
    }

    private void waitUntilServiceIsStarted() {
        await().ignoreExceptions()
                .pollInterval(SERVICE_WAITER_POLL_EVERY_SECONDS, TimeUnit.SECONDS)
                .atMost(SERVICE_WAITER_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .until(this::isManagedResourceRunning);
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
