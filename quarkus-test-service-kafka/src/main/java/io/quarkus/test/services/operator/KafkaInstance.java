package io.quarkus.test.services.operator;

import io.quarkus.test.bootstrap.OperatorService;
import io.quarkus.test.services.operator.model.KafkaInstanceCustomResource;

public class KafkaInstance extends OperatorService<KafkaInstance> {

    private static final String BOOTSTRAP_URL = "%s-kafka-bootstrap:9092";
    private static final String KAFKA_INSTANCE_NAME_DEFAULT = "kafka-instance";
    private static final String KAFKA_INSTANCE_TEMPLATE_DEFAULT = "/strimzi-operator-kafka-instance.yaml";

    private final String name;

    public KafkaInstance() {
        this(KAFKA_INSTANCE_NAME_DEFAULT, KAFKA_INSTANCE_TEMPLATE_DEFAULT);
    }

    public KafkaInstance(String name, String crdFile) {
        this.name = name;
        withCrd(name, crdFile, KafkaInstanceCustomResource.class);
    }

    public String getBootstrapUrl() {
        return String.format(BOOTSTRAP_URL, name);
    }
}
