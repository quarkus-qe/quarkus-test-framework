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

    default void onServiceInitiate(ExtensionContext context, Service service) {

    }

    default void onServiceError(ExtensionContext context, Service service, Throwable throwable) {

    }

    default void onServiceStarted(ExtensionContext context, Service service) {

    }

    default void onServiceStopped(ExtensionContext context, Service service) {

    }

    default void onSuccess(ExtensionContext context) {

    }

    default void beforeEach(ExtensionContext context) {

    }

    default void afterEach(ExtensionContext context) {

    }

    default Optional<Object> getParameter(Class<?> clazz) {
        return Optional.empty();
    }
}
