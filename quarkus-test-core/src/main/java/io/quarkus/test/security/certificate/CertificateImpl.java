package io.quarkus.test.security.certificate;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

record CertificateImpl(String keystorePath, String truststorePath, Map<String, String> configProperties,
        Collection<ClientCertificate> clientCertificates, String password, String format) implements Certificate {

    @Override
    public ClientCertificate getClientCertificateByCn(String cn) {
        Objects.requireNonNull(cn);
        if (clientCertificates() == null || clientCertificates().isEmpty()) {
            return null;
        }
        return clientCertificates()
                .stream()
                .filter(cc -> cn.equals(cc.commonName()))
                .findFirst()
                .orElse(null);
    }

}
