package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.utils.PropertiesUtils;

public class BaseServiceTest {

    @Test
    public void testWithSecretPropertyPrefixesValue() throws Exception {
        TestService service = new TestService();
        service.withSecretProperty("my.secret", "my-value");

        Field field = BaseService.class.getDeclaredField("staticProperties");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> properties = (Map<String, String>) field.get(service);

        String value = properties.get("my.secret");
        assertTrue(value.startsWith(PropertiesUtils.SECRET_LITERAL_PREFIX),
                "Property value should be prefixed with " + PropertiesUtils.SECRET_LITERAL_PREFIX);
        assertEquals(PropertiesUtils.SECRET_LITERAL_PREFIX + "my-value", value);
    }

    private static class TestService extends BaseService<TestService> {
        public TestService() {
            super();
        }
    }
}
