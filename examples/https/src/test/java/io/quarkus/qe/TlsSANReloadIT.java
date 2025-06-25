package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
public class TlsSANReloadIT {
    private static final String CERT_PREFIX = "reload-test";

    @QuarkusApplication(ssl = true, certificates = @Certificate(configureTruststore = true, configureHttpServer = true, configureKeystore = true, prefix = CERT_PREFIX))
    static final RestService app = new RestService()
            .withProperty("quarkus.tls.reload-period", "2s");

    @Test
    public void testRegenerateCertWithAlternativeNames() {
        int httpsPort = app.getURI(Protocol.HTTPS).getPort();
        // IP 127.0.0.1 (localhost) ir originally not listed in certificate, so request should fail on SSLCreation problem
        assertThrows(CompletionException.class,
                () -> app.mutinyHttps().get(httpsPort, "127.0.0.1", "/greeting").sendAndAwait(),
                "TLS connection should fail");

        // Add IP 127.0.0.1 as SAN so it should be trusted from now on
        app
                .<CertificateBuilder> getPropertyFromContext(CertificateBuilder.INSTANCE_KEY)
                .regenerateCertificate(CERT_PREFIX,
                        certReq -> certReq.withSubjectAlternativeNames(Arrays.asList("IP:127.0.0.1", "app", "randomString")));

        // after certificate is reloaded connection should be created as 127.0.0.1 is now in certificate
        AwaitilityUtils.untilAsserted(() -> {
            var response = app.mutinyHttps().get(httpsPort, "127.0.0.1", "/greeting").sendAndAwait();
            assertEquals("Hello World!", response.bodyAsString());
        });
    }
}
