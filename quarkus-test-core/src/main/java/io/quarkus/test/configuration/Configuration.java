package io.quarkus.test.configuration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public final class Configuration {

    private static final String TEST_PROPERTIES = "test.properties";
    private static final String APPREND = "ts.";

    private final Map<String, String> properties;

    private Configuration(Map<String, String> properties) {
        this.properties = properties;
    }

    public boolean isTrue(String property) {
        return is(property, Boolean.TRUE.toString());
    }

    public boolean is(String property, String expected) {
        boolean matches = false;
        for (Entry<String, String> entry : properties.entrySet()) {
            if (StringUtils.endsWith(entry.getKey(), property)) {
                matches = StringUtils.equalsIgnoreCase(entry.getValue(), expected);
                break;
            }
        }

        return matches;
    }

    public static final Configuration load(String serviceName) {
        Map<String, String> properties = new HashMap<>();

        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream(TEST_PROPERTIES)) {

            Properties prop = new Properties();
            prop.load(input);
            for (Entry<Object, Object> entry : prop.entrySet()) {
                String key = (String) entry.getKey();
                if (StringUtils.startsWith(key, APPREND + serviceName)) {
                    properties.put(key, (String) entry.getValue());
                }
            }

        } catch (Exception ex) {
            // There is no test.properties: this is not mandatory.
        }

        return new Configuration(properties);
    }
}
