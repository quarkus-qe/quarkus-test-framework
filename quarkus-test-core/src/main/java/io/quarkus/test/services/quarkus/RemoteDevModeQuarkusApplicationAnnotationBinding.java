package io.quarkus.test.services.quarkus;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.RemoteDevModeQuarkusApplication;

public class RemoteDevModeQuarkusApplicationAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(RemoteDevModeQuarkusApplication.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        RemoteDevModeQuarkusApplication metadata = field.getAnnotation(RemoteDevModeQuarkusApplication.class);

        ManagedResourceBuilder builder = new RemoteDevModeQuarkusApplicationManagedResourceBuilder();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return true;
    }

}
