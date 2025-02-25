package io.quarkus.test.security.certificate;

import io.vertx.core.buffer.Buffer;

public interface PemClientCertificate extends ClientCertificate {

    String keyPath();

    String certPath();

    boolean isEncrypted();

    String loadKeyCertificate();

    Buffer loadAndDecryptKeyCertificate();
}
