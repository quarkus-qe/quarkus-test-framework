package io.quarkus.test.bootstrap;

public class SqlServerService extends DatabaseService<SqlServerService> {

    private static final String USER = "sa";
    private static final String DEFAULT_PASSWORD = "My1337p@ssworD";
    private static final String DATABASE = "msdb";
    private static final String JDBC_NAME = "sqlserver";

    public SqlServerService() {
        super();
        withPassword(DEFAULT_PASSWORD);
    }

    @Override
    public String getUser() {
        return USER;
    }

    @Override
    public String getDatabase() {
        return DATABASE;
    }

    @Override
    public String getJdbcUrl() {
        var host = getURI();
        return "jdbc:" + getJdbcName() + "://" + host.getHost() + ":" + host.getPort() + ";databaseName=" + getDatabase();
    }

    @Override
    public SqlServerService withUser(String user) {
        throw new UnsupportedOperationException("You cannot configure a username for SQL Server");
    }

    @Override
    public SqlServerService withDatabase(String database) {
        throw new UnsupportedOperationException("You cannot configure a database for SQL Server");
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public SqlServerService onPreStart(Action action) {
        withProperty("SA_PASSWORD", getPassword());
        withProperty("ACCEPT_EULA", "Y");
        return super.onPreStart(action);
    }
}
