package io.quarkus.test.bootstrap;

public class OracleService extends DatabaseService<OracleService> {

    static final String USER_DEFAULT_VALUE = "myuser";
    static final String USER_PROPERTY = "APP_USER";
    static final String USER_PASSWORD_PROPERTY = "APP_USER_PASSWORD";
    static final String PASSWORD_PROPERTY = "ORACLE_PASSWORD";
    static final String DATABASE_PROPERTY = "ORACLE_DATABASE";
    static final String JDBC_NAME = "oracle";

    public OracleService() {
        // Oracle disallows to use "user", so we use "myuser" as default user name.
        withUser(USER_DEFAULT_VALUE);
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public String getJdbcUrl() {
        return getJdbcUrl(getDatabase());
    }

    /**
     * Creates JDBC URL pointing to the given database.
     *
     * @param databaseName database name
     * @return JDBC URL
     */
    public String getJdbcUrl(String databaseName) {
        var host = getURI();
        return "jdbc:" + getJdbcName() + ":thin:@" + host.getHost() + ":" + host.getPort() + "/" + databaseName;
    }

    @Override
    public String getReactiveUrl() {
        var host = getURI();
        return getJdbcName() + ":thin:@" + host.getHost() + ":" + host.getPort() + "/" + getDatabase();
    }

    @Override
    public OracleService onPreStart(Action action) {
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(USER_PASSWORD_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());

        return super.onPreStart(action);
    }
}
