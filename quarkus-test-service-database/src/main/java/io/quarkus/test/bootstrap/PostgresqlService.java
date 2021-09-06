package io.quarkus.test.bootstrap;

public class PostgresqlService extends DatabaseService<PostgresqlService> {

    static final String USER_PROPERTY = "POSTGRESQL_USER";
    static final String PASSWORD_PROPERTY = "POSTGRESQL_PASSWORD";
    static final String DATABASE_PROPERTY = "POSTGRESQL_DATABASE";
    static final String JDBC_NAME = "postgresql";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public PostgresqlService onPreStart(Action action) {
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());

        return super.onPreStart(action);
    }
}
