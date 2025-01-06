package io.quarkus.test.bootstrap.service.datagrid;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.service.OperatorOpenShiftInfinispan;

public class OperatorOpenShiftInfinispanAnnotationBinding implements AnnotationBinding {
    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(OperatorOpenShiftInfinispan.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        OperatorOpenShiftInfinispan metadata = field.getAnnotation(OperatorOpenShiftInfinispan.class);

        ManagedResourceBuilder builder = new OperatorOpenShiftInfinispanResourceBuilder();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return false;
    }
}
