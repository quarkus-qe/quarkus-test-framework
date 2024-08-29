package io.quarkus.test.security.certificate;

public record ClientCertificateRequest(String cnAttribute, boolean unknownToServer) {
}
