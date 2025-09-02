package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static io.quarkus.test.utils.StringUtils.sanitizeKubernetesObjectName;
import static io.quarkus.test.utils.TestExecutionProperties.isThisCliApp;
import static io.quarkus.test.utils.TestExecutionProperties.isThisStartedCliApp;
import static io.quarkus.test.utils.TestExecutionProperties.rememberThisAppStarted;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.LogsVerifier;
import io.quarkus.test.utils.PropertiesUtils;
import io.quarkus.test.utils.TestExecutionProperties;

public class BaseService<T extends Service> implements Service {

    public static final Duration SERVICE_STARTUP_TIMEOUT_DEFAULT = Duration.ofMinutes(5);
    private static final Duration SERVICE_STARTUP_CHECK_POLL_INTERVAL_DEFAULT = Duration.ofSeconds(2);

    protected ServiceContext context;
    private final ServiceLoader<ServiceListener> listeners = ServiceLoader.load(ServiceListener.class);
    private final List<Action> onPreStartActions = new LinkedList<>();
    private final List<Action> onPreStopActions = new LinkedList<>();
    private final List<Action> onPostStartActions = new LinkedList<>();
    private final Map<String, String> staticProperties = new HashMap<>();
    private final List<Runnable> futureProperties = new LinkedList<>();

    private ManagedResourceBuilder managedResourceBuilder;
    private ManagedResource managedResource;
    private String serviceName;
    private Configuration configuration;
    private boolean autoStart = true;

    public BaseService() {
        // add first
        this.onPreStartActions.add(s -> futureProperties.forEach(Runnable::run));
    }

    @Override
    public String getScenarioId() {
        return context.getScenarioId();
    }

    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public String getDisplayName() {
        return managedResource.getDisplayName();
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isAutoStart() {
        return autoStart;
    }

    public T onPreStart(Action action) {
        onPreStartActions.add(action);
        return (T) this;
    }

    public T onPreStop(Action action) {
        onPreStopActions.add(action);
        return (T) this;
    }

    public T onPostStart(Action action) {
        onPostStartActions.add(action);
        return (T) this;
    }

    public T setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperties(String... propertiesFiles) {
        if (context != null) {
            staticProperties.keySet().forEach(k -> context.getConfigPropertiesWithTestScope().remove(k));
        }
        staticProperties.clear();
        Stream.of(propertiesFiles).map(PropertiesUtils::toMap).forEach(properties -> {
            staticProperties.putAll(properties);
            updateTestScopeConfigProperties(properties);
        });
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     *
     * NOTE: unlike other {@link this::withProperties}, here we add new properties and keep the old ones
     */
    public T withProperties(Supplier<Map<String, String>> newProperties) {
        futureProperties.add(() -> context.getConfigPropertiesWithTestScope().putAll(newProperties.get()));
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    @Override
    public T withProperty(String key, String value) {
        this.staticProperties.put(key, value);
        updateTestScopeConfigProperties(Map.of(key, value));
        return (T) this;
    }

    /**
     * The runtime configuration property to be used if the built artifact is
     * configured to be run.
     */
    public T withProperty(String key, Supplier<String> value) {
        futureProperties.add(() -> context.withTestScopeConfigProperty(key, value.get()));
        return (T) this;
    }

    /**
     * The runtime configuration property to be configured based on type variable {@code U} from context.
     */
    public <U> T withProperty(String configKey, String contextKey, Function<U, String> configValue) {
        futureProperties.add(
                () -> context.withTestScopeConfigProperty(configKey, configValue.apply(getPropertyFromContext(contextKey))));
        return (T) this;
    }

    @Override
    public boolean isRunning() {
        Log.debug(this, "Checking if resource is running");
        if (managedResource == null) {
            Log.debug(this, "Resource is not running");
            return false;
        }

        Log.debug(this, "Resource is running");
        return managedResource.isRunning();
    }

    public URILike getURI(Protocol protocol) {
        return managedResource.getURI(protocol);
    }

    public URILike getURI() {
        return managedResource.getURI(Protocol.NONE);
    }

    @Deprecated
    public String getHost() {
        return getHost(Protocol.HTTP);
    }

    @Deprecated
    public String getHost(Protocol protocol) {
        return getURI(protocol).getRestAssuredStyleUri();
    }

    @Deprecated
    public Integer getPort() {
        return getPort(Protocol.HTTP);
    }

    @Deprecated
    public Integer getPort(Protocol protocol) {
        return getURI(protocol).getPort();
    }

    @Override
    public Map<String, String> getProperties() {
        if (context == null) {
            throw new IllegalStateException("Service properties requested before "
                    + "the 'io.quarkus.test.bootstrap.BaseService.register' has been called. "
                    + "We need to adjust this framework to register the scenario context first.");
        }
        return Collections.unmodifiableMap(context.getConfigPropertiesWithTestScope());
    }

    @Override
    public List<String> getLogs() {
        return new ArrayList<>(managedResource.logs());
    }

    @Override
    public String getProperty(String property, String defaultValue) {
        String value = getProperties().get(property);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        if (managedResourceBuilder != null) {
            String computedValue = managedResourceBuilder.getComputedProperty(property);
            if (StringUtils.isNotBlank(computedValue)) {
                return computedValue;
            }
        }

        return defaultValue;
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

        // our FW tries to start each auto-started service twice
        // I won't dare to change it until I have time to fix issues that I caused, but
        // TODO: we should figure out why BaseService:start is called more than once
        // once from the io.quarkus.test.bootstrap.QuarkusScenarioBootstrap.launchService
        // and once from the io.quarkus.test.bootstrap.QuarkusScenarioBootstrap.beforeEach
        // it doesn't matter for normal apps, but CLI app can launch and stop
        // so it won't be running on the next "start()"
        // so here, I am making sure that we remember the first start
        if (isThisStartedCliApp(context)) {
            return;
        } else {
            // we always need to remember this in case during the build we
            // recognize this is a CLI app, which happens later
            rememberThisAppStarted(context);
        }

        Log.debug(this, "Starting service (%s)", getDisplayName());
        onPreStartActions.forEach(a -> a.handle(this));
        doStart();
        waitUntilServiceIsStarted();
        onPostStartActions.forEach(a -> a.handle(this));
        Log.info(this, "Service started (%s)", getDisplayName());
    }

    /**
     * Stop the Quarkus application.
     */
    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }

        Log.debug(this, "Stopping service (%s)", getDisplayName());
        listeners.forEach(ext -> ext.onServiceStopped(context));
        onPreStopActions.forEach(a -> a.handle(this));
        managedResource.stop();

        Log.info(this, "Service stopped (%s)", getDisplayName());
    }

    /**
     * Let JUnit close remaining resources.
     */
    @Override
    public void close() {
        if (!context.getScenarioContext().isDebug()) {
            stop();
            if (getConfiguration().isTrue(Configuration.Property.DELETE_FOLDER_ON_EXIT)) {
                try {
                    FileUtils.deletePath(getServiceFolder());
                } catch (Exception ex) {
                    Log.warn(this, "Could not delete service folder. Caused by " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public ServiceContext register(String serviceName, ScenarioContext context) {
        if (TestExecutionProperties.isOpenshiftPlatform() || TestExecutionProperties.isKubernetesPlatform()) {
            return registerWithSanitizedServiceName(context, sanitizeKubernetesObjectName(serviceName), serviceName);
        }
        return registerWithSanitizedServiceName(context, serviceName, serviceName);
    }

    private ServiceContext registerWithSanitizedServiceName(ScenarioContext context, String serviceName,
            String originalServiceName) {
        this.serviceName = serviceName;
        if (serviceName.equals(originalServiceName)) {
            this.configuration = Configuration.load(serviceName);
        } else {
            this.configuration = Configuration.load(serviceName, originalServiceName);
        }

        this.context = createServiceContext(context);
        context.getTestStore().put(serviceName, this);
        this.context.getConfigPropertiesWithTestScope().putAll(this.staticProperties);

        return this.context;
    }

    protected ServiceContext createServiceContext(ScenarioContext context) {
        return new ServiceContext(this, context);
    }

    @Override
    public void init(ManagedResourceBuilder managedResourceBuilder) {
        FileUtils.recreateDirectory(context.getServiceFolder());
        this.managedResourceBuilder = managedResourceBuilder;
        this.managedResource = managedResourceBuilder.build(context);
        this.managedResource.validate();
        this.onPostStart((service) -> this.managedResource.afterStart());
    }

    public void restart() {
        managedResource.restart();
    }

    public void restartAndWaitUntilServiceIsStarted() {
        restart();
        waitUntilServiceIsStarted();
    }

    @Override
    public LogsVerifier logs() {
        return new LogsVerifier(this);
    }

    public Path getServiceFolder() {
        return context.getServiceFolder();
    }

    public <U> U getPropertyFromContext(String key) {
        if (context == null) {
            fail("Service has not been initialized yet. Make sure you invoke this method in the right order.");
        }

        return context.get(key);
    }

    private void doStart() {
        try {
            managedResource.start();
            listeners.forEach(ext -> ext.onServiceStarted(context));
        } catch (Throwable t) {
            listeners.forEach(ext -> ext.onServiceError(context, t));
            throw t;
        }
    }

    private boolean isRunningOrFailed() {
        if (managedResource != null && managedResource.isFailed()) {
            managedResource.stop();
            fail("Resource failed to start");
        }

        return isRunning();
    }

    private void waitUntilServiceIsStarted() {
        if (isThisCliApp(this.context)) {
            return;
        }
        try {
            Duration startupCheckInterval = getConfiguration()
                    .getAsDuration(Configuration.Property.SERVICE_STARTUP_CHECK_POLL_INTERVAL,
                            SERVICE_STARTUP_CHECK_POLL_INTERVAL_DEFAULT);
            Duration startupTimeout = getConfiguration()
                    .getAsDuration(Configuration.Property.SERVICE_STARTUP_TIMEOUT, SERVICE_STARTUP_TIMEOUT_DEFAULT);
            untilIsTrue(this::isRunningOrFailed, AwaitilitySettings
                    .using(startupCheckInterval, startupTimeout)
                    .doNotIgnoreExceptions()
                    .withService(this)
                    .timeoutMessage("Service didn't start in %s minutes", startupTimeout));
        } catch (Throwable t) {
            listeners.forEach(ext -> ext.onServiceError(context, t));
            throw t;
        }
    }

    private void updateTestScopeConfigProperties(Map<String, String> properties) {
        if (this.context != null) {
            // update the test context properties because this is probably happening "dynamically"
            // e.g. inside a test method before the service is restarted
            this.context.getConfigPropertiesWithTestScope().putAll(properties);
        }
    }
}
