package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.URILike;

@OpenShiftScenario
public class OpenShiftHttpsIT {
    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, configureTruststore = true, configureHttpServer = true, useTlsRegistry = false))
    static RestService app = new RestService();

    @Test
    public void basicHttpsTest() {
        assertEquals(
                "Hello World!",
                app.mutinyHttps().get("/greeting").sendAndAwait().bodyAsString(),
                "Https response text should match expected one");
    }

    @Test
    public void httpAndHttpsRoutePresentTest() {
        URILike httpRoute = app.getURI(Protocol.HTTP);
        assertEquals(80, httpRoute.getPort(), "Http port should be 80");
        assertEquals("http", httpRoute.getScheme(), "Http's route scheme should be http");

        URILike httpsRoute = app.getURI(Protocol.HTTPS);
        assertEquals(443, httpsRoute.getPort(), "Https port should be 443");
        assertEquals("https", httpsRoute.getScheme(), "Https routes' scheme should be https");
    }
}
