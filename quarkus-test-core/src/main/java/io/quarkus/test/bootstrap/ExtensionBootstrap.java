package io.quarkus.test.bootstrap;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface ExtensionBootstrap {

    boolean appliesFor(ExtensionContext context);

    default void beforeAll(ExtensionContext context) {

    }

    default void afterAll(ExtensionContext context) {

    }

    default void onError(ExtensionContext context, Throwable throwable) {

    }

    default void updateServiceContext(ServiceContext context) {

    }

    default Optional<Object> getParameter(Class<?> clazz) {
        return Optional.empty();
    }
}
