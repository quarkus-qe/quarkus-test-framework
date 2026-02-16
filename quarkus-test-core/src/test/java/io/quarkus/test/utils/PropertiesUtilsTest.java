package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PropertiesUtilsTest {

    @Test
    public void testIsSecret() {
        assertTrue(PropertiesUtils.isSecret(PropertiesUtils.SECRET_PREFIX + "any"));
        assertFalse(PropertiesUtils.isSecret("not-a-secret"));
        assertFalse(PropertiesUtils.isSecret(PropertiesUtils.SECRET_LITERAL_PREFIX + "literal-secret"));
    }

    @Test
    public void testIsSecretLiteral() {
        assertTrue(PropertiesUtils.isSecretLiteral(PropertiesUtils.SECRET_LITERAL_PREFIX + "any"));
        assertFalse(PropertiesUtils.isSecretLiteral("not-a-secret"));
        assertFalse(PropertiesUtils.isSecretLiteral(PropertiesUtils.SECRET_PREFIX + "file-secret"));
    }
}
