package io.quarkus.test.annotation;

import java.lang.reflect.Field;

import io.quarkus.test.AnnotationBinding;
import io.quarkus.test.ManagedResourceBuilder;

public class ContainerAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(Container.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        Container metadata = field.getAnnotation(Container.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

}
