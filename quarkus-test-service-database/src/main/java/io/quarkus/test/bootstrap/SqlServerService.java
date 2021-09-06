package io.quarkus.test.bootstrap;

public class SqlServerService extends DatabaseService<SqlServerService> {

    static final String USER = "sa";
    static final String PASSWORD = "My1337p@ssworD";
    static final String DATABASE = "mydb";
    static final String JDBC_NAME = "sqlserver";

    @Override
    public String getUser() {
        return USER;
    }

    @Override
    public String getPassword() {
        return PASSWORD;
    }

    @Override
    public String getDatabase() {
        return DATABASE;
    }

    @Override
    public String getJdbcUrl() {
        return getHost().replace("http", "jdbc:" + getJdbcName()) + ":" + getPort() + ";" + getDatabase();
    }

    @Override
    public SqlServerService withUser(String user) {
        throw new UnsupportedOperationException("No supported using SQL Server");
    }

    @Override
    public SqlServerService withPassword(String password) {
        throw new UnsupportedOperationException("No supported using Sql Server");
    }

    @Override
    public SqlServerService withDatabase(String database) {
        throw new UnsupportedOperationException("No supported using Sql Server");
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }
}
