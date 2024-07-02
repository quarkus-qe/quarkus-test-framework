package io.quarkus.test.services.containers.strimzi;

import java.util.ArrayList;
import java.util.List;

import org.testcontainers.images.builder.Transferable;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.test.logging.Log;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.strimzi.test.container.StrimziKafkaContainer;

/**
 * Extend the functionality of io.strimzi.StrimziKafkaContainer with:
 * - Do not overwrite parameters of server.properties.
 */
public class ExtendedStrimziKafkaContainer extends StrimziKafkaContainer {

    private static final String KAFKA_MAPPED_PORT = "${KAFKA_MAPPED_PORT}";
    private static final int ALLOW_EXEC = 700;
    private static final String TESTCONTAINERS_SCRIPT = "/testcontainers_start.sh";

    private boolean useCustomServerProperties = false;

    public ExtendedStrimziKafkaContainer(String name, String version) {
        super(String.format("%s:%s", name, version));
    }

    public void useCustomServerProperties() {
        useCustomServerProperties = true;
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        Log.info("Starting container using custom server properties");
        if (useCustomServerProperties) {
            Log.info("Starting container using custom server properties");
            List<String> script = new ArrayList<>();
            script.add("#!/bin/bash");
            script.add("set -euv");
            int kafkaExposedPort = this.getMappedPort(KafkaVendor.STRIMZI.getPort());
            script.add("sed 's/" + KAFKA_MAPPED_PORT + "/" + kafkaExposedPort + "/g' "
                    + "config/kraft/server.properties  > /tmp/effective_server.properties");
            script.add("KAFKA_CLUSTER_ID=\"$(bin/kafka-storage.sh random-uuid)\"");
            String storageFormat = "/opt/kafka/bin/kafka-storage.sh format"
                    + " -t=${KAFKA_CLUSTER_ID}"
                    + " -c /tmp/effective_server.properties";
            script.add(storageFormat);
            script.add("bin/kafka-server-start.sh /tmp/effective_server.properties");
            this.copyFileToContainer(Transferable.of(String.join("\n", script), ALLOW_EXEC), TESTCONTAINERS_SCRIPT);
        } else {
            Log.info("Starting container using standard server properties");
            // we do not process credentials here, since SASL always used together with custom properties
            // see StrimziKafkaContainerManagedResource#getServerProperties
            super.containerIsStarting(containerInfo, reused);
            Log.info("Starting container using standard server properties and cluster id " + super.getClusterId());
            // if that is to change, we will need to copy script from test containers, modify it and copy back again
        }
    }

    /**
     * The code below requires an explanation.
     * StrimziKafkaContainer has a special method which makes it use kraft mode (without a zookeeper)
     * Container quay.io/strimzi/kafka requires for broker.id and node.id to have the same value in kraft mode,
     * and for some reason strimzi class always overwrites broker id (to 0 by default)
     * since config/kraft/server.properties contains node.id=1, we have to use this value
     */
    public ExtendedStrimziKafkaContainer enableKraftMode() {
        return (ExtendedStrimziKafkaContainer) super.withKraft()
                .withBrokerId(1);
    }
}
