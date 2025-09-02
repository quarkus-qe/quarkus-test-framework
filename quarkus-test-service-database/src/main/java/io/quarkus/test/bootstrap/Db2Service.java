package io.quarkus.test.bootstrap;

public class Db2Service extends DatabaseService<Db2Service> {

    static final String USER_PROPERTY = "DB2INSTANCE";
    static final String PASSWORD_PROPERTY = "DB2INST1_PASSWORD";
    static final String DATABASE_PROPERTY = "DBNAME";
    static final String JDBC_NAME = "db2";

    public Db2Service() {
        withProperty("AUTOCONFIG", "false");
        withProperty("ARCHIVE_LOGS", "false");
        withProperty("LICENSE", "accept");
        onPreStart(service -> service
                .withProperty(USER_PROPERTY, getUser())
                .withProperty(PASSWORD_PROPERTY, getPassword())
                .withProperty(DATABASE_PROPERTY, getDatabase()));
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

}
