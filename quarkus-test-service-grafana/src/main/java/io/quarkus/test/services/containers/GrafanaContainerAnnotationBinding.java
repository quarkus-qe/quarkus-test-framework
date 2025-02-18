package io.quarkus.test.services.containers;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.GrafanaContainer;

public class GrafanaContainerAnnotationBinding implements AnnotationBinding {
    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(GrafanaContainer.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        GrafanaContainer metadata = field.getAnnotation(GrafanaContainer.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return true;
    }
}
