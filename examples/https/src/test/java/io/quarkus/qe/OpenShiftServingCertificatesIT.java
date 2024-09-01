package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.qe.hero.Hero;
import io.quarkus.qe.hero.HeroClient;
import io.quarkus.qe.hero.HeroClientResource;
import io.quarkus.qe.hero.HeroResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

/**
 * Test OpenShift serving certificate support provided by our framework.
 */
@DisabledOnNative // building 2 apps is costly and point here is to test FW support not Quarkus
@OpenShiftScenario
public class OpenShiftServingCertificatesIT {

    private static final String CLIENT_TLS_CONFIG_NAME = "cert-serving-test-client";
    private static final String SERVER_TLS_CONFIG_NAME = "cert-serving-test-server";

    @QuarkusApplication(ssl = true, certificates = @Certificate(tlsConfigName = SERVER_TLS_CONFIG_NAME, servingCertificates = @Certificate.ServingCertificates(addServiceCertificate = true)), classes = {
            HeroResource.class, Hero.class })
    static final RestService server = new RestService()
            .withProperty("quarkus.http.ssl.client-auth", "request")
            .withProperty("quarkus.http.insecure-requests", "DISABLED");

    @QuarkusApplication(certificates = @Certificate(tlsConfigName = CLIENT_TLS_CONFIG_NAME, servingCertificates = @Certificate.ServingCertificates(injectCABundle = true)), classes = {
            HeroClient.class, Hero.class, HeroClientResource.class })
    static final RestService client = new RestService()
            .withProperty("quarkus.rest-client.hero.tls-configuration-name", CLIENT_TLS_CONFIG_NAME)
            .withProperty("quarkus.rest-client.hero.uri", () -> server.getURI(Protocol.HTTPS).getRestAssuredStyleUri());

    @Test
    public void testSecuredCommunicationBetweenClientAndServer() {
        // REST client use OpenShift internal CA
        // server is configured with OpenShift serving certificates
        // ad "untilAsserted": hopefully it's not necessary, but once I experienced unknown SAN,
        // so to avoid flakiness I am adding here retry:
        AwaitilityUtils.untilAsserted(() -> {
            var hero = client.given()
                    .get("hero-client-resource")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(Hero.class);
            assertNotNull(hero);
            assertNotNull(hero.name());
            assertTrue(hero.name().startsWith("Name-"));
            assertTrue(hero.otherName().startsWith("Other-"));
        });
    }

}
