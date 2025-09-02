package io.quarkus.test.bootstrap;

public class PostgresqlService extends DatabaseService<PostgresqlService> {

    /**
     * Red Hat PostgreSQL container image at certain point redirects logs to a file.
     * This is name of property that determines destination.
     * More info here:
     * https://github.com/sclorg/postgresql-container/blob/master/16/root/usr/share/container-scripts/postgresql/README.md
     */
    static final String LOG_DESTINATION = "POSTGRESQL_LOG_DESTINATION";
    static final String USER_PROPERTY = "POSTGRESQL_USER";
    static final String PASSWORD_PROPERTY = "POSTGRESQL_PASSWORD";
    static final String DATABASE_PROPERTY = "POSTGRESQL_DATABASE";
    static final String JDBC_NAME = "postgresql";

    // DockerHub environment variables
    static final String DH_USER_PROPERTY = "POSTGRES_USER";
    static final String DH_PASSWORD_PROPERTY = "POSTGRES_PASSWORD";
    static final String DH_DATABASE_PROPERTY = "POSTGRES_DB";

    public PostgresqlService() {
        onPreStart(service -> service
                // RedHat registry environment variables
                .withProperty(USER_PROPERTY, getUser())
                .withProperty(PASSWORD_PROPERTY, getPassword())
                .withProperty(DATABASE_PROPERTY, getDatabase())
                .withProperty(LOG_DESTINATION, "/dev/stdout")

                // DockerHub environment variables
                .withProperty(DH_USER_PROPERTY, getUser())
                .withProperty(DH_PASSWORD_PROPERTY, getPassword())
                .withProperty(DH_DATABASE_PROPERTY, getDatabase()));
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

}
