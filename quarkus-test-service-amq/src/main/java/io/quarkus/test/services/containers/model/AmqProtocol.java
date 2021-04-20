package io.quarkus.test.services.containers.model;

public enum AmqProtocol {
    TCP(61616),
    AMQP(5672);

    private final int port;

    AmqProtocol(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
