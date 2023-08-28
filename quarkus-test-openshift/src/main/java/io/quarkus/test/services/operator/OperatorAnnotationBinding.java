package io.quarkus.test.services.operator;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.Operator;

public class OperatorAnnotationBinding implements AnnotationBinding {
    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(Operator.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        Operator metadata = field.getAnnotation(Operator.class);

        ManagedResourceBuilder builder = new OperatorManagedResourceBuilder();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return false;
    }
}
