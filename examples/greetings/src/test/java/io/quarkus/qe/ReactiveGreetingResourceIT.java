package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;

@QuarkusScenario
public class ReactiveGreetingResourceIT {

    @QuarkusApplication
    static final RestService app = new RestService();

    @Test
    public void shouldSayDefaultGreeting() {
        String response = app.mutiny().get("/reactive-greeting")
                .expect(ResponsePredicate.status(HttpURLConnection.HTTP_OK))
                .send()
                .map(HttpResponse::bodyAsString)
                .await().indefinitely();

        assertEquals("Hello, I'm victor", response);
    }
}
