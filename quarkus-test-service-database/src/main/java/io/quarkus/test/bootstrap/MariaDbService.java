package io.quarkus.test.bootstrap;

public class MariaDbService extends DatabaseService<MariaDbService> {

    static final String USER_PROPERTY = "MARIADB_USER";
    static final String PASSWORD_PROPERTY = "MARIADB_PASSWORD";
    static final String PASSWORD_ROOT_PROPERTY = "MARIADB_ROOT_PASSWORD";
    static final String DATABASE_PROPERTY = "MARIADB_DATABASE";
    static final String JDBC_NAME = "mariadb";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public MariaDbService onPreStart(Action action) {
        // Some MariaDB images need MariaDB, some others MySql ones... So, we provide both:
        // - MySQL properties
        withProperty(MySqlService.USER_PROPERTY, getUser());
        withProperty(MySqlService.PASSWORD_PROPERTY, getPassword());
        withProperty(MySqlService.PASSWORD_ROOT_PROPERTY, getPassword());
        withProperty(MySqlService.DATABASE_PROPERTY, getDatabase());
        // - MariaDB properties
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(PASSWORD_ROOT_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());

        return super.onPreStart(action);
    }
}
