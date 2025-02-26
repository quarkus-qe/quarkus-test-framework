package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class HttpsEncryptedPemIT {

    private static final String DUMMY_CLIENT_CN_1 = "my-dummy--1";
    @QuarkusApplication(ssl = true, certificates = @Certificate(format = Certificate.Format.ENCRYPTED_PEM, configureHttpServer = true, clientCertificates = {
            @Certificate.ClientCertificate(cnAttribute = DUMMY_CLIENT_CN_1)
    }))
    static final RestService app = new RestService()
            .withProperty("quarkus.http.insecure-requests", "disabled")
            .withProperty("quarkus.http.ssl.client-auth", "none");

    @Test
    public void testSimpleHttpsCommunication() {
        var response = app.mutinyHttps(DUMMY_CLIENT_CN_1).get("/greeting").sendAndAwait();
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("Hello World!", response.bodyAsString(),
                "Response is not the expected on that endpoint");
    }
}
