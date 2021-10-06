package io.quarkus.test.services.containers.strimzi;

import java.nio.charset.StandardCharsets;

import org.testcontainers.images.builder.Transferable;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.test.services.containers.model.KafkaVendor;
import io.strimzi.StrimziKafkaContainer;

/**
 * Extend the functionality of io.strimzi.StrimziKafkaContainer with:
 * - Do not overwrite parameters of server.properties.
 *
 */
public class ExtendedStrimziKafkaContainer extends StrimziKafkaContainer {

    private static final String KAFKA_MAPPED_PORT = "${KAFKA_MAPPED_PORT}";
    private static final int ALLOW_EXEC = 700;

    private boolean useCustomServerProperties = false;

    public ExtendedStrimziKafkaContainer(String version) {
        super(version);
    }

    public void useCustomServerProperties() {
        useCustomServerProperties = true;
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        if (useCustomServerProperties) {
            int kafkaExposedPort = this.getMappedPort(KafkaVendor.STRIMZI.getPort());

            String command = "#!/bin/bash \n";
            command = command + "sed 's/" + KAFKA_MAPPED_PORT + "/" + kafkaExposedPort + "/g' "
                    + "config/server.properties > /tmp/effective_server.properties &\n";
            command = command + "bin/zookeeper-server-start.sh config/zookeeper.properties &\n";
            command = command + "bin/kafka-server-start.sh /tmp/effective_server.properties";
            this.copyFileToContainer(Transferable.of(command.getBytes(StandardCharsets.UTF_8), ALLOW_EXEC),
                    "/testcontainers_start.sh");
        } else {
            super.containerIsStarting(containerInfo, reused);
        }
    }
}
