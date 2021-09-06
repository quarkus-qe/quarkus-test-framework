package io.quarkus.test.bootstrap;

public class Db2Service extends DatabaseService<Db2Service> {

    static final String USER_PROPERTY = "DB2INSTANCE";
    static final String PASSWORD_PROPERTY = "DB2INST1_PASSWORD";
    static final String DATABASE_PROPERTY = "DBNAME";
    static final String JDBC_NAME = "db2";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public Db2Service onPreStart(Action action) {
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());
        withProperty("AUTOCONFIG", "false");
        withProperty("ARCHIVE_LOGS", "false");
        withProperty("LICENSE", "accept");

        return super.onPreStart(action);
    }
}
