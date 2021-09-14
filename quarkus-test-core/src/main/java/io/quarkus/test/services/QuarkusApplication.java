package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusApplication {
    // By default, it will load all the classes in the classpath.
    Class<?>[] classes() default {};

    Class<? extends ManagedResourceBuilder> builder() default ProdQuarkusApplicationManagedResourceBuilder.class;

    /**
     * Add forced dependencies.
     */
    Dependency[] dependencies() default {};

    /**
     * Enable SSL configuration. This property needs `quarkus.http.ssl.certificate.key-store-file` and
     * `quarkus.http.ssl.certificate.key-store-password` to be set.
     */
    boolean ssl() default false;

    /**
     * Enable GRPC configuration. This property will map the gPRC service to a random port.
     */
    boolean grpc() default false;
}
