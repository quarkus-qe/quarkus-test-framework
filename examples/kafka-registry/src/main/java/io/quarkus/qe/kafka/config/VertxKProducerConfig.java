package io.quarkus.qe.kafka.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "vertx.kafka.producer")
public class VertxKProducerConfig {

    @WithName("delay-ms")
    public long delay;

    @WithName("batchSize")
    public int batchSize;

}
