package io.quarkus.test.annotation;

import java.lang.reflect.Field;

import io.quarkus.test.AnnotationBinding;
import io.quarkus.test.ManagedResourceBuilder;

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
