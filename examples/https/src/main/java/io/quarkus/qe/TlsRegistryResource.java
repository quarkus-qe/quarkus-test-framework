package io.quarkus.qe;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

@Path("/tls-registry")
public class TlsRegistryResource {

    private static final String CERT_EXAMPLE = "dummy-entry-0";

    @Inject
    TlsConfigurationRegistry tlsConfigurationRegistry;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String tlsRegistryInspection() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration tlsConfiguration = tlsConfigurationRegistry.getDefault().orElseThrow();

        KeyStore keyStore = tlsConfiguration.getKeyStore();
        if (keyStore == null) {
            return "No KeyStore found in default TLS configuration.";
        }
        X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(CERT_EXAMPLE);
        if (x509Certificate == null) {
            return "No certificate found with alias " + CERT_EXAMPLE;
        }
        return "Subject X500 : " + x509Certificate.getSubjectX500Principal().getName()
                + "\nSubject Alternative names (SANs) : " + x509Certificate.getSubjectAlternativeNames();
    }

}
