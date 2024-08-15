package io.quarkus.test.util;

import static java.util.Collections.singletonMap;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

public abstract class YamlPropertiesHandler {

    public static void writePropertiesIntoYaml(File yamlFile, Properties properties) throws IOException {
        Yaml yaml = new Yaml();
        yaml.dump(properties, new FileWriter(yamlFile));
    }

    public static Properties readYamlFileIntoProperties(File yamlFile) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(new FileInputStream(yamlFile));

        Properties properties = new Properties();
        properties.putAll(getFlattenedMap(obj));

        return properties;
    }

    private static Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        source.forEach((key, value) -> {
            if (!isBlank(path)) {
                key = path + (key.startsWith("[") ? key : '.' + key);
            }
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                buildFlattenedMap(result, (Map<String, Object>) value, key);
            } else if (value instanceof Collection) {
                int count = 0;
                for (Object object : (Collection<?>) value) {
                    buildFlattenedMap(result, singletonMap("[" + (count++) + "]", object), key);
                }
            } else {
                result.put(key, value != null ? "" + value : "");
            }
        });
    }
}
