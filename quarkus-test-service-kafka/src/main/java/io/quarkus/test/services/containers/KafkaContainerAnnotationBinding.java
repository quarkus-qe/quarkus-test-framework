package io.quarkus.test.services.containers;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.KafkaContainer;

public class KafkaContainerAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(KafkaContainer.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) throws Exception {
        KafkaContainer metadata = field.getAnnotation(KafkaContainer.class);

        ManagedResourceBuilder builder = metadata.builder().getDeclaredConstructor().newInstance();
        builder.init(metadata);
        return builder;
    }

}
