package io.quarkus.qe.debug;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusScenario
public class HelloIT {

    @QuarkusApplication
    static final RestService app = new RestService(false);

    @Test
    public void test() {
        String response = app.mutiny().get("/hello")
                .expect(ResponsePredicate.status(HttpURLConnection.HTTP_OK))
                .send()
                .map(HttpResponse::bodyAsString)
                .await().indefinitely();
        assertEquals("hello", response);
    }

}
