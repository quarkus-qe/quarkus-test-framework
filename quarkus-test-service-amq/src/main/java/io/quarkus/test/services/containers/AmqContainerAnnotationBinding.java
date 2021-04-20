package io.quarkus.test.services.containers;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.AmqContainer;

public class AmqContainerAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(AmqContainer.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        AmqContainer metadata = field.getAnnotation(AmqContainer.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

}
