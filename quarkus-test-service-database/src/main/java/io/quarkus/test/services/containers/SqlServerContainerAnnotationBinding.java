package io.quarkus.test.services.containers;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.SqlServerContainer;

public class SqlServerContainerAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(SqlServerContainer.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        SqlServerContainer metadata = field.getAnnotation(SqlServerContainer.class);
        ManagedResourceBuilder builder = new SqlServerManagedResourceBuilder();
        builder.init(metadata);
        return builder;
    }

    @Override
    public boolean requiresLinuxContainersOnBareMetal() {
        return true;
    }
}
