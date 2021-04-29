package io.quarkus.qe.kafka.producer;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.qe.kafka.model.LoginAttempt;
import io.quarkus.qe.kafka.streams.WindowedLoginDeniedStream;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;

@ApplicationScoped
public class LoginEventsProducer {

    private static final int SEND_EVENT_EVERY_MILLIS = 1000;

    @ConfigProperty(name = "producer.httpCodes")
    List<Integer> httpCodes;
    @ConfigProperty(name = "producer.loginUrls")
    List<String> loginUrls;

    private final Random random = new Random();

    @Outgoing(WindowedLoginDeniedStream.LOGIN_ATTEMPTS_TOPIC)
    public Multi<Record<String, String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(SEND_EVENT_EVERY_MILLIS))
                .onOverflow().drop()
                .map(tick -> {
                    String loginEndpoint = getRandomEndpointUrl();
                    String loginEndpointEnc = encodeId(loginEndpoint);
                    Integer httpCode = getRandomHttpCode();

                    return Record.of(loginEndpointEnc,
                            Json.encode(new LoginAttempt(loginEndpointEnc, loginEndpoint, httpCode)));
                });
    }

    private String getRandomEndpointUrl() {
        return loginUrls.get(random.nextInt(loginUrls.size()));
    }

    private Integer getRandomHttpCode() {
        return httpCodes.get(random.nextInt(httpCodes.size()));
    }

    private String encodeId(String id) {
        return Base64.getEncoder().encodeToString(id.getBytes());
    }

}
