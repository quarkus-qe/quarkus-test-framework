package io.quarkus.test.bootstrap;

public enum Protocol {
    HTTP("http", 8080),
    HTTPS("https", 8443),
    GRPC("grpc", 9000);

    private String value;
    private int port;

    Protocol(String value, int port) {
        this.value = value;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getValue() {
        return value;
    }
}
