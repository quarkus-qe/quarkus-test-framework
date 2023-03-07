package io.quarkus.qe;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.JaegerService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.services.JaegerContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
//TODO https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0#opentelemetry
@DisabledOnQuarkusSnapshot(reason = "HTTP span names are now \"{http.method} {http.route}\" instead of just \"{http.route}\"")
public class ClientResourceIT {

    private static final String SERVICE_NAME = "test-traced-service";
    private static final String CLIENT_ENDPOINT = "/client";

    @JaegerContainer()
    static JaegerService jaeger = new JaegerService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", jaeger::getCollectorUrl);

    @Test
    public void shouldUpdateJaegerAsTracer() {
        app.given()
                .get(CLIENT_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("I'm a client"));

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> given()
                .queryParam("service", SERVICE_NAME)
                .queryParam("operation", CLIENT_ENDPOINT)
                .get(jaeger.getTraceUrl())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(1))
                .body("data[0].spans", hasSize(1))
                .body("data[0].spans.operationName", hasItems(CLIENT_ENDPOINT))
                .body("data[0].spans.logs.fields.value.flatten()", hasItems("ClientResource called")));
    }
}
