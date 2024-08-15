package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.test.util.YamlPropertiesHandler;

public class YamlPropertiesHandlerTest {

    @Test
    public void testYamlParsing() throws URISyntaxException, IOException {
        File yamlFile = new File(getClass().getClassLoader().getResource("example.yaml").toURI());
        Properties properties = YamlPropertiesHandler.readYamlFileIntoProperties(yamlFile);

        assertEquals("http://localhost:8180/auth/realms/quarkus", properties.getProperty("quarkus.oidc.auth-server-url"),
                "Loaded property should have the value from yaml");
    }

    @Test
    public void testYamlWriteAndRead() throws IOException {
        File tempYamlFile = File.createTempFile("_yamlTest", ".yaml");
        tempYamlFile.deleteOnExit();

        Properties properties = new Properties();
        properties.put("quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy", "sync");
        properties.put("quarkus.hibernate-search-orm.quarkusQE.automatic-indexing.synchronization.strategy", "sync");

        YamlPropertiesHandler.writePropertiesIntoYaml(tempYamlFile, properties);

        Properties parsedProperties = YamlPropertiesHandler.readYamlFileIntoProperties(tempYamlFile);

        assertEquals(properties, parsedProperties, "Parsed properties should be the same, as those written to the file");
    }
}
