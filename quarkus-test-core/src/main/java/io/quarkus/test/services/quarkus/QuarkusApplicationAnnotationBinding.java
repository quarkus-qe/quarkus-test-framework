package io.quarkus.test.services.quarkus;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.QuarkusApplication;

public class QuarkusApplicationAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(QuarkusApplication.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        QuarkusApplication metadata = field.getAnnotation(QuarkusApplication.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

}
