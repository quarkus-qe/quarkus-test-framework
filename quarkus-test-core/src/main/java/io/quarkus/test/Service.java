package io.quarkus.test;

import static io.restassured.RestAssured.given;
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
import io.restassured.specification.RequestSpecification;

public class Service {

    private static final int SERVICE_WAITER_POLL_EVERY_SECONDS = 2;
    private static final int SERVICE_WAITER_TIMEOUT_MINUTES = 5;

    private final String serviceName;
    private final List<Action> onPreStartActions = new LinkedList<>();
    private final List<Action> onPostStartActions = new LinkedList<>();
    private final Map<String, String> properties = new HashMap<>();
    private final List<Runnable> futureProperties = new LinkedList<>();

    private ManagedResource managedResource;
    private Configuration configuration;

    public Service(String name) {
        this.serviceName = name;
        this.configuration = Configuration.load(serviceName);
        onPreStart(s -> futureProperties.forEach(Runnable::run));
    }

    public String getName() {
        return serviceName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Service onPreStart(Action action) {
        onPreStartActions.add(action);
        return this;
    }

    public Service onPostStart(Action action) {
        onPostStartActions.add(action);
        return this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public Service withProperties(String propertiesFile) {
        properties.clear();
        properties.putAll(PropertiesUtils.toMap(propertiesFile));
        return this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public Service withProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public Service withProperty(String key, Supplier<String> value) {
        futureProperties.add(() -> properties.put(key, value.get()));
        return this;
    }

    public String getHost() {
        return managedResource.getHost();
    }

    public Integer getPort() {
        return managedResource.getPort();
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Start the managed resource. If the managed resource is running, it does
     * nothing.
     *
     * @throws RuntimeException when application errors at startup.
     */
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
    public void stop() {
        if (!isManagedResourceRunning()) {
            return;
        }

        Log.debug(this, "Stopping service");
        managedResource.stop();

        Log.info(this, "Service stopped");
    }

    public RequestSpecification restAssured() {
        return given().baseUri(managedResource.getHost()).basePath("/").port(managedResource.getPort());
    }

    protected void init(ManagedResourceBuilder managedResourceBuilder, ServiceContext context) {
        Log.info(this, "Initialize service");
        FileUtils.recreateDirectory(context.getServiceFolder());
        managedResource = managedResourceBuilder.build(context);
    }

    private void waitUntilServiceIsStarted() {
        await().pollInterval(SERVICE_WAITER_POLL_EVERY_SECONDS, TimeUnit.SECONDS)
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
