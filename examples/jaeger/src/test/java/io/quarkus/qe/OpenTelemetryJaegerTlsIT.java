package io.quarkus.qe;

import static io.quarkus.test.services.containers.JaegerGenericDockerContainerManagedResource.CERTIFICATE_CONTEXT_KEY;
import static io.quarkus.test.services.containers.JaegerGenericDockerContainerManagedResource.JAEGER_CLIENT_CERT_CN;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.JaegerService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.security.certificate.PemClientCertificate;
import io.quarkus.test.services.JaegerContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class OpenTelemetryJaegerTlsIT {

    private static final String SERVICE_NAME = "test-traced-service";
    private static final String CLIENT_ENDPOINT = "/client";
    private static final String OPERATION = "GET " + CLIENT_ENDPOINT;

    @JaegerContainer(tls = true)
    static JaegerService jaeger = new JaegerService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.otel.exporter.otlp.traces.tls-configuration-name", "jaeger")
            .withProperty("quarkus.otel.exporter.otlp.traces.endpoint", () -> jaeger.getCollectorUrl(Protocol.HTTPS))
            .withProperty("quarkus.tls.jaeger.key-store.pem.0.cert", () -> getClientCert().certPath())
            .withProperty("quarkus.tls.jaeger.key-store.pem.0.key", () -> getClientCert().keyPath())
            .withProperty("quarkus.tls.jaeger.trust-store.pem.certs", () -> getClientCert().truststorePath())
            .withProperty("quarkus.tls.jaeger.trust-all", "false"); // it's default, but let's make it clear

    private static PemClientCertificate getClientCert() {
        return (PemClientCertificate) jaeger.<Certificate.PemCertificate> getPropertyFromContext(CERTIFICATE_CONTEXT_KEY)
                .getClientCertificateByCn(JAEGER_CLIENT_CERT_CN);
    }

    @Test
    public void shouldUpdateJaegerAsTracer() {
        app.given()
                .get(CLIENT_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("I'm a client"));

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> given()
                .queryParam("service", SERVICE_NAME)
                .queryParam("operation", OPERATION)
                .get(jaeger.getTraceUrl())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(1))
                .body("data[0].spans", hasSize(1))
                .body("data[0].spans.operationName", hasItems(OPERATION))
                .body("data[0].spans.logs.fields.value.flatten()", hasItems("ClientResource called")));
    }
}
