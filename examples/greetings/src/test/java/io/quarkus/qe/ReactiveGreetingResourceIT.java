package io.quarkus.qe;

import static io.smallrye.mutiny.vertx.core.Expectations.expectation;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.mutiny.ext.web.client.HttpResponse;

@QuarkusScenario
public class ReactiveGreetingResourceIT {

    @QuarkusApplication
    static final RestService app = new RestService();

    @Test
    public void shouldSayDefaultGreeting() {
        String response = app.mutiny().get("/reactive-greeting")
                .send()
                .plug(expectation(HttpResponse::getDelegate, HttpResponseExpectation.status(200)))
                .map(HttpResponse::bodyAsString)
                .await().indefinitely();

        assertEquals("Hello, I'm victor", response);
    }
}
