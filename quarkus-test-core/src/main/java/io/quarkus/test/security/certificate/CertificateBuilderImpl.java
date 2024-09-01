package io.quarkus.test.security.certificate;

import java.util.List;
import java.util.Objects;

record CertificateBuilderImpl(List<Certificate> certificates,
        ServingCertificateConfig servingCertificateConfig) implements CertificateBuilder {
    @Override
    public Certificate findCertificateByPrefix(String prefix) {
        Objects.requireNonNull(prefix);
        return certificates.stream().filter(c -> prefix.equals(c.prefix())).findFirst().orElse(null);
    }

    @Override
    public Certificate regenerateCertificate(String prefix, CertificateRequestCustomizer... customizers) {
        if (findCertificateByPrefix(prefix) instanceof InterchangeableCertificate cert) {
            var req = new CertificateRequestCustomizer.CertificateRequestImpl(cert.getCertRequestOptions());
            for (CertificateRequestCustomizer customizer : customizers) {
                customizer.customize(req);
            }
            var newCert = Certificate.ofRegeneratedCert(req.createNewOptions());
            return cert.swapCert(newCert);
        } else {
            throw new IllegalStateException("Certificate must be interchangeable when regeneration is required");
        }
    }

}
