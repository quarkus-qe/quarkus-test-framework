package io.quarkus.test.services.quarkus;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.DevModeQuarkusApplication;

public class DevModeQuarkusApplicationAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(DevModeQuarkusApplication.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        DevModeQuarkusApplication metadata = field.getAnnotation(DevModeQuarkusApplication.class);

        ManagedResourceBuilder builder = new DevModeQuarkusApplicationManagedResourceBuilder();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return true;
    }

}
