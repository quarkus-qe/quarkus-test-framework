package io.quarkus.qe.resources;

import org.testcontainers.containers.GenericContainer;

public class PrometheusPushGatewayContainer extends GenericContainer<PrometheusPushGatewayContainer> {

    public static final int REST_PORT = 9091;

    public PrometheusPushGatewayContainer() {
        super("prom/pushgateway");
        addFixedExposedPort(REST_PORT, REST_PORT);
    }
}
