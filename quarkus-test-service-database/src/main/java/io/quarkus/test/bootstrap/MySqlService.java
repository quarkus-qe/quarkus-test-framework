package io.quarkus.test.bootstrap;

public class MySqlService extends DatabaseService<MySqlService> {

    static final String USER_PROPERTY = "MYSQL_USER";
    static final String PASSWORD_PROPERTY = "MYSQL_PASSWORD";
    static final String PASSWORD_ROOT_PROPERTY = "MYSQL_ROOT_PASSWORD";
    static final String DATABASE_PROPERTY = "MYSQL_DATABASE";
    static final String JDBC_NAME = "mysql";

    public MySqlService() {
        onPreStart(service -> service
                .withProperty(USER_PROPERTY, getUser())
                .withProperty(PASSWORD_PROPERTY, getPassword())
                .withProperty(PASSWORD_ROOT_PROPERTY, getPassword())
                .withProperty(DATABASE_PROPERTY, getDatabase()));
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

}
