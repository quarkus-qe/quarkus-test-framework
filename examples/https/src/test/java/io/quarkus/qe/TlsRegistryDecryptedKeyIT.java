package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class TlsRegistryDecryptedKeyIT {
    private static final String CERT_EXAMPLE = "dummy-entry-0";

    @QuarkusApplication(ssl = true, certificates = @Certificate(format = Certificate.Format.ENCRYPTED_PEM, configureHttpServer = true, clientCertificates = @Certificate.ClientCertificate(cnAttribute = CERT_EXAMPLE)))
    static final RestService app = new RestService()
            .withProperty("quarkus.http.insecure-requests", "disabled")
            .withProperty("quarkus.http.ssl.client-auth", "none");

    @Test
    public void testInspectDecryptedKey() {
        var response = app.mutinyHttps(CERT_EXAMPLE).get("/tls-registry").sendAndAwait();
        assertEquals(HttpStatus.SC_OK, response.statusCode(), "Expected 200 but got: "
                + response.statusCode());

        String body = response.bodyAsString();
        assertTrue(body.contains("Subject X500 : CN=dummy-entry-0"),
                "Response from /tls-inspection does not contain subject info: " + body);
        assertTrue(body.contains("localhost") || body.contains(" [[2, localhost], [2, 0.0.0.0]]"),
                "Response from /tls-inspection does not mention Subject Alternative names (SANs) : localhost or  [[2, localhost], [2, 0.0.0.0]]");
    }
}
