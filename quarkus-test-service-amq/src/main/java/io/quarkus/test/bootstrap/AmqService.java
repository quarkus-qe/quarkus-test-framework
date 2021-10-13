package io.quarkus.test.bootstrap;

import io.quarkus.test.utils.FileUtils;

public class AmqService extends BaseService<AmqService> {
    private static final String AMQ_USER = "quarkus";
    private static final String AMQ_PASSWORD = "quarkus";

    public AmqService() {
        withProperty("AMQ_USER", AMQ_USER);
        withProperty("AMQ_PASSWORD", AMQ_PASSWORD);
        withProperty("BROKER_XML", FileUtils.loadFile("/broker.xml"));
    }

    public String getAmqUser() {
        return AMQ_USER;
    }

    public String getAmqPassword() {
        return AMQ_PASSWORD;
    }

    public String getTcpUrl() {
        return getURI().withScheme("tcp").toString();
    }

    public String getAmqpUrl() {
        return getURI().withScheme("amqp").toString();
    }

    public String getAmqpHost() {
        return getURI().getHost();
    }

    public Integer getPort() {
        return getURI().getPort();
    }
}
