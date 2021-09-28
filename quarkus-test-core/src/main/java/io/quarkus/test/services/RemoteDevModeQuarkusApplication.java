package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteDevModeQuarkusApplication {
    String password() default "qe";

    /**
     * @return the properties file to use to configure the Quarkus application.
     */
    String properties() default "application.properties";
}
