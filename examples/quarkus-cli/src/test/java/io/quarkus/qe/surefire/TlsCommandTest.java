package io.quarkus.qe.surefire;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashSet;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.tls.TlsConfigurationRegistry;

/**
 * This test is only supported to run inside QuarkusCliTlsCommandIT.
 */
@QuarkusTest
public class TlsCommandTest {

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testKeystoreInDefaultTlsRegistry() throws KeyStoreException {
        var defaultRegistry = registry.getDefault()
                .orElseThrow(() -> new AssertionError("Default TLS Registry is not configured"));
        var ks = defaultRegistry.getKeyStore();
        var ksAliasesSet = new HashSet<String>();
        var ksAliases = ks.aliases();
        while (ksAliases.hasMoreElements()) {
            ksAliasesSet.add(ksAliases.nextElement());
        }
        // if this changes to something sensible, it's not an issue
        // basically what we try to assert here is that:
        // 1. keystore is configured
        // 2. it has some aliases that can be used
        Assertions.assertTrue(ksAliasesSet.contains("dev-certificate"));
        Assertions.assertTrue(ksAliasesSet.contains("issuer-dev-certificate"));

        try {
            // check we do know password
            var key = ks.getKey("dev-certificate", "quarkus".toCharArray());
            Assertions.assertNotNull(key);
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

}
