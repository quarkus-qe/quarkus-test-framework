package io.quarkus.test.extensions;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.ServiceContext;

public interface ExtensionBootstrap {

    boolean appliesFor(ExtensionContext context);

    default void beforeAll(ExtensionContext context) {

    }

    default void afterAll(ExtensionContext context) {

    }

    default void updateServiceContext(ServiceContext context) {

    }

}
