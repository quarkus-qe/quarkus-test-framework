package io.quarkus.qe;

import static io.quarkus.qe.HttpsTlsRegistryNamedConfigIT.CLIENT_CN_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.security.certificate.ClientCertificateRequest;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
public class TlsRegistryCertificateReloadingIT {

    private static final String CERT_PREFIX = "reload-test";
    private static final String NEW_CLIENT_CN = "my-new-client";

    @QuarkusApplication(ssl = true, certificates = @Certificate(clientCertificates = {
            @Certificate.ClientCertificate(cnAttribute = CLIENT_CN_1)
    }, configureTruststore = true, configureHttpServer = true, configureKeystore = true, prefix = CERT_PREFIX))
    static final RestService app = new RestService()
            .withProperty("quarkus.http.ssl.client-auth", "request")
            .withProperty("quarkus.http.insecure-requests", "DISABLED")
            .withProperty("quarkus.tls.reload-period", "2s");

    @Test
    public void testCertificateReload() {
        var path = "/greeting/mutual-tls";

        var response = app.mutinyHttps(CLIENT_CN_1).get(path).sendAndAwait();
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Hello CN=%s!".formatted(CLIENT_CN_1), response.bodyAsString());

        var clientReq = new ClientCertificateRequest(NEW_CLIENT_CN, false);
        app
                .<CertificateBuilder> getPropertyFromContext(CertificateBuilder.INSTANCE_KEY)
                .regenerateCertificate(CERT_PREFIX, certReq -> certReq.withClientRequests(clientReq));

        AwaitilityUtils.untilAsserted(() -> {
            var response1 = app.mutinyHttps(NEW_CLIENT_CN).get(path).sendAndAwait();
            assertEquals(HttpStatus.SC_OK, response1.statusCode());
            assertEquals("Hello CN=%s!".formatted(NEW_CLIENT_CN), response1.bodyAsString());
        });
    }
}
