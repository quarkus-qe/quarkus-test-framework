package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class ReactiveGreetingResourceIT {

    @QuarkusApplication
    static final RestService app = new RestService();

    @Test
    public void shouldSayDefaultGreeting() {
        String response = app.mutiny().get("/reactive-greeting")
                .send()
                .map(resp -> {
                    assertEquals(HttpURLConnection.HTTP_OK, resp.statusCode(), "Expected HTTP OK status");
                    return resp.bodyAsString();
                })
                .await().indefinitely();

        assertEquals("Hello, I'm victor", response);
    }
}
