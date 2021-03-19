package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public final class PropertiesUtils {
    private PropertiesUtils() {

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map<String, String> toMap(String propertiesFile) {
        Properties properties = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
            properties.load(in);
        } catch (IOException e) {
            fail("Could not start extension. Caused by " + e);
        }

        return (Map) properties;
    }

}
