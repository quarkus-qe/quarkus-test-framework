package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MtlsEncryptedPemTlsRegistryIT {

    static final String CLIENT_CN_1 = "my-client-1";

    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, clientCertificates = {
            @Certificate.ClientCertificate(cnAttribute = CLIENT_CN_1)
    }, configureTruststore = true, configureHttpServer = true, format = Certificate.Format.ENCRYPTED_PEM))
    static final RestService app = new RestService()
            .withProperty("quarkus.http.ssl.client-auth", "required")
            .withProperty("quarkus.http.insecure-requests", "disabled");

    @Test
    public void testMutualTLS() {
        var path = "/greeting/mutual-tls";
        var response = app.mutinyHttps(CLIENT_CN_1).get(path).sendAndAwait();
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Hello CN=%s!".formatted(CLIENT_CN_1), response.bodyAsString());
    }
}
