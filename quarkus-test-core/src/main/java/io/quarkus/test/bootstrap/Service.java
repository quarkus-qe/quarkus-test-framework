package io.quarkus.test.bootstrap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.utils.LogsVerifier;

public interface Service extends ExtensionContext.Store.CloseableResource {

    String getName();

    Configuration getConfiguration();

    Map<String, String> getProperties();

    List<String> getLogs();

    ServiceContext register(String serviceName, ExtensionContext testContext);

    void init(ManagedResourceBuilder resource);

    void start();

    void stop();

    boolean isRunning();

    LogsVerifier logs();

    Service withProperty(String key, String value);

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
