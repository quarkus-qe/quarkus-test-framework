package io.quarkus.test.configuration;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public final class Configuration {

    private static final String GLOBAL_PROPERTIES = System.getProperty("ts.test.resources.file.location", "global.properties");
    private static final String TEST_PROPERTIES = "test.properties";
    private static final String PREFIX_TEMPLATE = "ts.%s.";
    private static final String GLOBAL_SCOPE = "global";

    private final Map<String, String> properties;

    private Configuration(Map<String, String> properties) {
        this.properties = properties;
    }

    public Duration getAsDuration(String property, Duration defaultValue) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }

        return Duration.parse(value);
    }

    public Double getAsDouble(String property, double defaultValue) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        return Double.parseDouble(value);
    }

    public String get(String property) {
        return properties.get(property);
    }

    public String getOrDefault(String property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }

    public boolean isTrue(String property) {
        return is(property, Boolean.TRUE.toString());
    }

    public boolean is(String property, String expected) {
        return StringUtils.equalsIgnoreCase(properties.get(property), expected);
    }

    public static Configuration load() {
        Map<String, String> properties = new HashMap<>();
        // Lowest priority: properties from global.properties and scope `global`
        properties.putAll(loadPropertiesFrom(GLOBAL_PROPERTIES, GLOBAL_SCOPE));
        // Then, properties from system properties and scope `global`
        properties.putAll(loadPropertiesFromSystemProperties(GLOBAL_SCOPE));
        // Then, properties from test.properties and scope as global
        properties.putAll(loadPropertiesFrom(TEST_PROPERTIES, GLOBAL_SCOPE));

        return new Configuration(properties);
    }

    public static Configuration load(String serviceName) {
        Configuration configuration = load();
        // Then, properties from test.properties and scope as service name
        configuration.properties.putAll(loadPropertiesFrom(TEST_PROPERTIES, serviceName));
        // Then, highest priority: properties from system properties and scope as service name
        configuration.properties.putAll(loadPropertiesFromSystemProperties(serviceName));

        return configuration;
    }

    private static Map<String, String> loadPropertiesFromSystemProperties(String scope) {
        return loadPropertiesFrom(System.getProperties(), scope);
    }

    private static Map<String, String> loadPropertiesFrom(String propertiesFile, String scope) {
        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            Properties prop = new Properties();
            prop.load(input);
            return loadPropertiesFrom(prop, scope);
        } catch (Exception ignored) {
            // There is no properties file: this is not mandatory.
        }

        return Collections.emptyMap();
    }

    private static Map<String, String> loadPropertiesFrom(Properties prop, String scope) {
        Map<String, String> properties = new HashMap<>();
        String prefix = String.format(PREFIX_TEMPLATE, scope);
        for (Entry<Object, Object> entry : prop.entrySet()) {
            String key = (String) entry.getKey();
            if (StringUtils.startsWith(key, prefix)) {
                properties.put(key.replace(prefix, StringUtils.EMPTY), (String) entry.getValue());
            }
        }

        return properties;
    }
}
