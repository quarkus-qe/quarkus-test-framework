package io.quarkus.test.bootstrap;

public abstract class DatabaseService<T extends Service> extends BaseService<T> {

    static final String USER_DEFAULT_VALUE = "user";
    static final String PASSWORD_DEFAULT_VALUE = "user";
    static final String DATABASE_DEFAULT_VALUE = "mydb";

    private String user = USER_DEFAULT_VALUE;
    private String password = PASSWORD_DEFAULT_VALUE;
    private String database = DATABASE_DEFAULT_VALUE;

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public String getJdbcUrl() {
        return getHost().replace("http", "jdbc:" + getJdbcName()) + ":" + getPort() + "/" + getDatabase();
    }

    public String getReactiveUrl() {
        return getHost().replace("http", getJdbcName()) + ":" + getPort() + "/" + getDatabase();
    }

    public T with(String user, String password, String database) {
        withUser(user);
        withPassword(password);
        withDatabase(database);
        return (T) this;
    }

    public T withUser(String user) {
        this.user = user;
        return (T) this;
    }

    public T withPassword(String password) {
        this.password = password;
        return (T) this;
    }

    public T withDatabase(String database) {
        this.database = database;
        return (T) this;
    }

    protected abstract String getJdbcName();
}
