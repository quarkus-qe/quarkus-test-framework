package io.quarkus.qe;

import static io.restassured.RestAssured.get;
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
import io.quarkus.test.services.JaegerContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class ClientResourceIT {

    static final String SERVICE_NAME = "test-traced-service";

    @JaegerContainer
    static JaegerService jaeger = new JaegerService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.jaeger.service-name", SERVICE_NAME)
            .withProperty("quarkus.jaeger.endpoint", jaeger::getRestUrl);

    @Test
    public void shouldUpdateJaegerAsTracer() {
        app.given()
                .get("/client")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("I'm a client"));
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Response response = get(jaeger.getTraceUrl() + "?service=" + SERVICE_NAME);
            response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .body("data", hasSize(1))
                    .body("data[0].spans", hasSize(1))
                    .body("data[0].spans.operationName", hasItems(
                            "GET:io.quarkus.qe.ClientResource.get"))
                    .body("data[0].spans.logs.fields.value.flatten()", hasItems(
                            "ClientResource called"));
        });
    }
}
