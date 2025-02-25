package io.quarkus.test.security.certificate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.smallrye.certs.pem.parsers.EncryptedPKCS8Parser;
import io.vertx.core.buffer.Buffer;

record ClientCertificateImpl(String commonName, String keystorePath, String truststorePath, String keyPath,
        String certPath, boolean isEncrypted, String password) implements PemClientCertificate {

    ClientCertificateImpl(String commonName, String keystorePath, String truststorePath) {
        this(commonName, keystorePath, truststorePath, null, null, false, null);
    }

    @Override
    public String loadKeyCertificate() {
        byte[] content;
        try (InputStream is = Files.newInputStream(Path.of(keyPath))) {
            content = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read client certificate", e);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public Buffer loadAndDecryptKeyCertificate() {
        if (!isEncrypted()) {
            throw new IllegalStateException("Client certificate is not encrypted");
        }
        Buffer decrypted = new EncryptedPKCS8Parser().decryptKey(loadKeyCertificate(), password);
        if (decrypted == null) {
            throw new IllegalArgumentException("Unable to decrypt the key file: " + keyPath());
        }
        return decrypted;
    }
}
