package io.quarkus.test.configuration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public final class Configuration {

    private static final String TEST_PROPERTIES = "test.properties";
    private static final String PREFIX_TEMPLATE = "ts.%s.";

    private final Map<String, String> properties;

    private Configuration(Map<String, String> properties) {
        this.properties = properties;
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

    public static Configuration load(String serviceName) {
        Map<String, String> properties = new HashMap<>();

        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream(TEST_PROPERTIES)) {

            Properties prop = new Properties();
            prop.load(input);
            String servicePrefix = String.format(PREFIX_TEMPLATE, serviceName);
            for (Entry<Object, Object> entry : prop.entrySet()) {
                String key = (String) entry.getKey();
                if (StringUtils.startsWith(key, servicePrefix)) {
                    properties.put(key.replace(servicePrefix, StringUtils.EMPTY), (String) entry.getValue());
                }
            }

        } catch (Exception ex) {
            // There is no test.properties: this is not mandatory.
        }

        return new Configuration(properties);
    }
}
