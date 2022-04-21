package io.quarkus.test.bootstrap;

public class PostgresqlService extends DatabaseService<PostgresqlService> {

    static final String USER_PROPERTY = "POSTGRESQL_USER";
    static final String PASSWORD_PROPERTY = "POSTGRESQL_PASSWORD";
    static final String DATABASE_PROPERTY = "POSTGRESQL_DATABASE";
    static final String JDBC_NAME = "postgresql";

    // DockerHub environment variables
    static final String DH_USER_PROPERTY = "POSTGRES_USER";
    static final String DH_PASSWORD_PROPERTY = "POSTGRES_PASSWORD";
    static final String DH_DATABASE_PROPERTY = "POSTGRES_DB";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public PostgresqlService onPreStart(Action action) {
        // RedHat registry environment variables
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());

        // DockerHub environment variables
        withProperty(DH_USER_PROPERTY, getUser());
        withProperty(DH_PASSWORD_PROPERTY, getPassword());
        withProperty(DH_DATABASE_PROPERTY, getDatabase());

        return super.onPreStart(action);
    }
}
