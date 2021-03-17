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

import org.jboss.logging.Logger;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.restassured.specification.RequestSpecification;

public class Service {

    private static final Logger LOG = Logger.getLogger(Service.class);

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
     * configured to be run
     */
    public Service withProperties(String propertiesFile) {
        properties.clear();
        properties.putAll(PropertiesUtils.toMap(propertiesFile));
        return this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run
     */
    public Service withProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run
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
        LOG.infof("[%s] Starting service", getName());

        onPreStartActions.forEach(a -> a.handle(this));
        managedResource.start();
        waitUntilServiceIsStarted();
        onPostStartActions.forEach(a -> a.handle(this));
        LOG.infof("[%s] Service started", getName());
    }

    /**
     * Stop the Quarkus application.
     */
    public void stop() {
        LOG.infof("[%s] Stopping service", getName());
        if (managedResource != null) {
            managedResource.stop();
        }

        LOG.infof("[%s] Service stopped", getName());
    }

    public RequestSpecification restAssured() {
        return given().baseUri(managedResource.getHost()).basePath("/").port(managedResource.getPort());
    }

    protected void init(ManagedResourceBuilder managedResourceBuilder, ServiceContext context) {
        LOG.infof("[%s] Initialize service", getName());
        FileUtils.recreateDirectory(context.getServiceFolder());
        managedResource = managedResourceBuilder.build(context);
    }

    private void waitUntilServiceIsStarted() {
        await().pollInterval(4, TimeUnit.SECONDS).atMost(5, TimeUnit.MINUTES).until(this::isManagedResourceRunning);
    }

    private boolean isManagedResourceRunning() {
        LOG.debugf("[%s] Checking if resource is running", getName());
        boolean isRunning = managedResource.isRunning();
        if (isRunning) {
            LOG.debugf("[%s] Resource is running!", getName());
        } else {
            LOG.debugf("[%s] Resource is not running yet", getName());
        }

        return isRunning;
    }
}
