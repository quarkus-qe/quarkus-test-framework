package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class HttpsIT {

    private static final String CLIENT_CN_1 = "my-client-1";
    private static final String CLIENT_CN_2 = "my-client-2";
    private static final String CLIENT_CN_3 = "my-client-3";

    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, configureTruststore = true, clientCertificates = {
            @Certificate.ClientCertificate(cnAttribute = CLIENT_CN_1),
            @Certificate.ClientCertificate(cnAttribute = CLIENT_CN_2),
            @Certificate.ClientCertificate(cnAttribute = CLIENT_CN_3, unknownToServer = true)
    }))
    static final RestService app = new RestService()
            .withProperty("quarkus.http.ssl.client-auth", "request")
            .withProperty("quarkus.http.insecure-requests", "DISABLED");

    @Test
    public void testTLS() {
        // mTLS auth is not required, but the communication must be secured
        sayHello();
    }

    @Test
    public void testMutualTLS() {
        // mTLS required
        sayHello(CLIENT_CN_1);
        sayHello(CLIENT_CN_2);
    }

    @Test
    public void testFailureAsServerUnknownToClient() {
        try {
            app.mutinyHttps(true, CLIENT_CN_1, false).get("/greeting/mutual-tls").sendAndAwait();
        } catch (Exception e) {
            return;
        }
        Assertions.fail("HTTP request should had failed as server is unknown to the client");
    }

    @Test
    public void testAuthNFailureAsClientUnknownToServer() {
        var resp = app.mutinyHttps(CLIENT_CN_3).get("/greeting/mutual-tls").sendAndAwait();
        assertEquals(401, resp.statusCode());
    }

    private static void sayHello() {
        sayHello(CLIENT_CN_1, false);
    }

    private static void sayHello(String clientCn) {
        sayHello(clientCn, true);
    }

    private static void sayHello(String clientCn, boolean requireMutualTLS) {
        var path = "/greeting" + (requireMutualTLS ? "/mutual-tls" : "");
        var response = app.mutinyHttps(clientCn).get(path).sendAndAwait();
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        if (requireMutualTLS) {
            assertEquals("Hello CN=%s!".formatted(clientCn), response.bodyAsString());
        } else {
            assertEquals("Hello World!", response.bodyAsString());
        }
    }
}
