package io.quarkus.test.security.certificate;

import java.util.Collection;
import java.util.Map;

public final class InterchangeableCertificate implements Certificate.PemCertificate {

    private volatile CertificateOptions certRequestOptions;
    private volatile Certificate.PemCertificate delegate;

    private InterchangeableCertificate(PemCertificate delegate, CertificateOptions certRequestOptions) {
        this.delegate = delegate;
        this.certRequestOptions = certRequestOptions;
    }

    static InterchangeableCertificate wrapCert(Certificate.PemCertificate certificate, CertificateOptions options) {
        return new InterchangeableCertificate(certificate, options);
    }

    InterchangeableCertificate swapCert(Certificate.PemCertificate certificate) {
        this.delegate = certificate;
        return this;
    }

    public CertificateOptions getCertRequestOptions() {
        return certRequestOptions;
    }

    @Override
    public String prefix() {
        return delegate.prefix();
    }

    @Override
    public String format() {
        return delegate.format();
    }

    @Override
    public String password() {
        return delegate.password();
    }

    @Override
    public String keystorePath() {
        return delegate.keystorePath();
    }

    @Override
    public String truststorePath() {
        return delegate.truststorePath();
    }

    @Override
    public Map<String, String> configProperties() {
        return delegate.configProperties();
    }

    @Override
    public ClientCertificate getClientCertificateByCn(String cn) {
        return delegate.getClientCertificateByCn(cn);
    }

    @Override
    public Collection<ClientCertificate> clientCertificates() {
        return delegate.clientCertificates();
    }

    @Override
    public String keyPath() {
        return delegate.keyPath();
    }

    @Override
    public String certPath() {
        return delegate.certPath();
    }
}
