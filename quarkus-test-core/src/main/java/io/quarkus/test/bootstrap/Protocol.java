package io.quarkus.test.bootstrap;

public enum Protocol {
    HTTP("http", 8080),
    HTTPS("https", 8443),
    GRPC("grpc", 9000),
    MANAGEMENT("management", 9000), //can be either http or https
    WS("ws", 8080),
    WSS("wss", 8443),
    NONE(null, -1);

    private final String value;
    private final int port;

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
