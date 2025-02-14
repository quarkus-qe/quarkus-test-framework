package io.quarkus.test.services;

public @interface Mount {
    String from() default "";

    String to() default "";
}
