package io.quarkus.test.bootstrap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.utils.LogsVerifier;

public interface Service {

    String getName();

    Configuration getConfiguration();

    Map<String, String> getProperties();

    List<String> getLogs();

    void register(String name);

    void init(ManagedResourceBuilder resource, ServiceContext serviceContext);

    void start();

    void stop();

    LogsVerifier logs();

    Service withProperty(String key, String value);

    default void validate(Field field) {

    }
}
