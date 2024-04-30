package io.quarkus.test.security.certificate;

import java.util.List;

record CertificateBuilderImp(List<Certificate> certificates) implements CertificateBuilder {
}
