package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GitRepositoryQuarkusApplication {
    String repo();

    String branch() default "";

    String contextDir() default "";

    String mavenArgs() default "-DskipTests=true -DskipITs=true "
            + "-Dquarkus.platform.version=${QUARKUS_VERSION} "
            + "-Dquarkus-plugin.version=${QUARKUS-PLUGIN_VERSION} "
            + "-Dquarkus.platform.group-id=${QUARKUS_PLATFORM_GROUP-ID}";

    boolean devMode() default false;

    String artifact() default "";

    /**
     * @return the properties file to use to configure the Quarkus application.
     */
    String properties() default "application.properties";
}
