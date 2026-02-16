package io.quarkus.test.bootstrap.inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.utils.PropertiesUtils;

public class KubectlClientTest {

    @Test
    public void testIsSecretLiteral() {
        assertTrue(PropertiesUtils.isSecretLiteral(PropertiesUtils.SECRET_LITERAL_PREFIX + "any"));
        assertFalse(PropertiesUtils.isSecretLiteral("not-a-secret"));
        assertFalse(PropertiesUtils.isSecretLiteral(PropertiesUtils.SECRET_PREFIX + "file-secret"));
    }
}
