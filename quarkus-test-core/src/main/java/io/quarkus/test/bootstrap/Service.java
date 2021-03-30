package io.quarkus.test.bootstrap;

import java.util.Map;

import io.quarkus.test.configuration.Configuration;

public interface Service {

    String getName();

    Configuration getConfiguration();

    Map<String, String> getProperties();

    void register(String name);

    void init(ManagedResourceBuilder resource, ServiceContext serviceContext);

    void start();

    void stop();

    Service withProperty(String key, String value);
}
