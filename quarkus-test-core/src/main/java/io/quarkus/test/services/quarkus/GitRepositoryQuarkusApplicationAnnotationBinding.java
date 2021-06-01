package io.quarkus.test.services.quarkus;

import java.lang.reflect.Field;

import io.quarkus.test.bootstrap.AnnotationBinding;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

public class GitRepositoryQuarkusApplicationAnnotationBinding implements AnnotationBinding {

    @Override
    public boolean isFor(Field field) {
        return field.isAnnotationPresent(GitRepositoryQuarkusApplication.class);
    }

    @Override
    public ManagedResourceBuilder createBuilder(Field field) {
        GitRepositoryQuarkusApplication metadata = field.getAnnotation(GitRepositoryQuarkusApplication.class);

        ManagedResourceBuilder builder = new GitRepositoryQuarkusApplicationManagedResourceBuilder();
        builder.init(metadata);
        return builder;
    }

}
