package io.quarkus.test.bootstrap;

public class MySqlService extends DatabaseService<MySqlService> {

    static final String USER_PROPERTY = "MYSQL_USER";
    static final String PASSWORD_PROPERTY = "MYSQL_PASSWORD";
    static final String PASSWORD_ROOT_PROPERTY = "MYSQL_ROOT_PASSWORD";
    static final String DATABASE_PROPERTY = "MYSQL_DATABASE";
    static final String JDBC_NAME = "mysql";
    private static final String FIPS_OPTIONS = "useSSL=false&allowPublicKeyRetrieval=True";

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

    @Override
    public MySqlService onPreStart(Action action) {
        withProperty(USER_PROPERTY, getUser());
        withProperty(PASSWORD_PROPERTY, getPassword());
        withProperty(PASSWORD_ROOT_PROPERTY, getPassword());
        withProperty(DATABASE_PROPERTY, getDatabase());

        return super.onPreStart(action);
    }

    public String getJdbcUrl() {
        return getURI()
                .withScheme("jdbc:" + getJdbcName())
                .withPath("/" + getDatabase())
                .withQuery(FIPS_OPTIONS)
                .toString();
    }

    public String getReactiveUrl() {
        return getURI()
                .withScheme(getJdbcName())
                .withPath("/" + getDatabase())
                .withQuery(FIPS_OPTIONS)
                .toString();
    }
}
