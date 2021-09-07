package io.quarkus.test.bootstrap;

public class MongoDbService extends DatabaseService<MongoDbService> {
    static final String JDBC_NAME = "mongodb";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public String getJdbcUrl() {
        return getHost().replace("http", getJdbcName()) + ":" + getPort();
    }

    @Override
    public String getReactiveUrl() {
        return getJdbcUrl();
    }

    @Override
    public String getUser() {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public String getDatabase() {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public MongoDbService with(String user, String password, String database) {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public MongoDbService withUser(String user) {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public MongoDbService withPassword(String password) {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }

    @Override
    public MongoDbService withDatabase(String database) {
        throw new UnsupportedOperationException("No supported using MongoDB");
    }
}
