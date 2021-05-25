package io.quarkus.qe;

import static io.quarkus.qe.resources.JaegerContainer.REST_PORT;
import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItems;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.qe.resources.JaegerContainer;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TracingExtensionBootstrapIT {

    static final String MAIN_SERVICE_NAME = "quarkus-test-framework";

    static JaegerContainer jaegerContainer = new JaegerContainer();

    @QuarkusApplication
    static RestService app = new RestService();

    @BeforeAll
    static public void tearUp() {
        jaegerContainer.start();
    }

    @AfterAll
    static public void tearDown() {
        jaegerContainer.stop();
    }

    @Test
    @Order(1)
    public void successExample() {
        // This test is needed by 'shouldTraceSuccessTest', because will generate a trace on Jaeger
        Assertions.assertTrue(true);
    }

    @Test
    @Order(2)
    public void shouldTraceSuccessTest() {
        // depends on 'successExample' test
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> get("http://localhost:" + REST_PORT + "/api/traces?service=" + MAIN_SERVICE_NAME)
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data[0].spans.find { it.operationName == 'TracingExtensionBootstrapIT_successExample' }.tags.collect { \"${it.key}=${it.value}\".toString() }",
                                hasItems(
                                        "success=true",
                                        "bare-metal=true",
                                        "buildNumber=777-default",
                                        "versionNumber=999-default")));
    }

    @Test
    @Disabled("It would make the whole test suite fail")
    @Order(3)
    public void errorExample() {
        // This test is needed by 'shouldTraceErrorTest', because will generate a trace on Jaeger
        Assertions.fail();
    }

    @Test
    @Disabled("Depends on errorExample")
    @Order(4)
    public void shouldTraceErrorTest() {
        // depends on 'errorExample' test
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> get("http://localhost:" + REST_PORT + "/api/traces?service=" + MAIN_SERVICE_NAME)
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data[0].spans.find { it.operationName == 'TracingExtensionBootstrapIT_errorExample' }.tags.collect { \"${it.key}=${it.value}\".toString() }",
                                hasItems(
                                        "error=true",
                                        "bare-metal=true",
                                        "buildNumber=777-default",
                                        "versionNumber=999-default")));
    }
}
