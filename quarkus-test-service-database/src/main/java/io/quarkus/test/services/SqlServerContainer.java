package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlServerContainer {

    String image() default "${mssql.image}";

    int port() default 1433;

    String expectedLog() default "Service Broker manager has started";

    /**
     * Encrypt connections to SQL Server.
     */
    boolean tlsEnabled() default false;
}
