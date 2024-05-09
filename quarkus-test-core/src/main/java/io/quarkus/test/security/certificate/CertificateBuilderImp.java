package io.quarkus.test.security.certificate;

import java.util.List;
import java.util.Objects;

record CertificateBuilderImp(List<Certificate> certificates) implements CertificateBuilder {
    @Override
    public Certificate findCertificateByPrefix(String prefix) {
        Objects.requireNonNull(prefix);
        return certificates.stream().filter(c -> prefix.equals(c.prefix())).findFirst().orElse(null);
    }
}
