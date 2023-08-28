package io.quarkus.test.services.containers;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.JaegerContainer;

public class JaegerContainerAnnotationBinding implements AnnotationBinding {
    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(JaegerContainer.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        JaegerContainer metadata = field.getAnnotation(JaegerContainer.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return true;
    }
}
