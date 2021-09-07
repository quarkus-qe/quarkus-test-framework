package io.quarkus.test.bootstrap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.utils.LogsVerifier;

public interface Service extends ExtensionContext.Store.CloseableResource {

    String getScenarioId();

    String getName();

    String getDisplayName();

    Configuration getConfiguration();

    Map<String, String> getProperties();

    default Optional<String> getProperty(String property) {
        return Optional.ofNullable(getProperty(property, null));
    }

    String getProperty(String property, String defaultValue);

    List<String> getLogs();

    ServiceContext register(String serviceName, ScenarioContext context);

    void init(ManagedResourceBuilder resource);

    void start();

    void stop();

    boolean isRunning();

    LogsVerifier logs();

    Service withProperty(String key, String value);

    default boolean isAutoStart() {
        return true;
    }

    default void validate(Field field) {

    }

    /**
     * Let JUnit close remaining resources.
     */
    @Override
    default void close() {
        stop();
    }
}
