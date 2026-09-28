package io.quarkus.qe.debug;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusScenario
public class HelloIT {

    @QuarkusApplication
    static final RestService app = new RestService(false);

    @Test
    public void test() {
        HttpResponse<?> response = app.mutiny().get("/hello")
                .send()
                .await().indefinitely();
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("hello", response.bodyAsString());
    }

}
