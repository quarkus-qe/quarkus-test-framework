package io.quarkus.test.bootstrap;

import org.apache.commons.lang3.StringUtils;

public class AmqService extends BaseService<AmqService> {
    private static final String AMQ_USER = "quarkus";
    private static final String AMQ_PASSWORD = "quarkus";

    public AmqService() {
        withProperty("AMQ_USER", AMQ_USER);
        withProperty("AMQ_PASSWORD", AMQ_PASSWORD);
    }

    public String getAmqUser() {
        return AMQ_USER;
    }

    public String getAmqPassword() {
        return AMQ_PASSWORD;
    }

    public String getTcpUrl() {
        return String.format("%s:%s", getHost().replace("http", "tcp"), getPort());
    }

    public String getAmqpUrl() {
        return String.format("%s:%s", getHost().replace("http", "amqp"), getPort());
    }

    public String getAmqpHost() {
        return getHost().replace("http://", StringUtils.EMPTY);
    }
}
