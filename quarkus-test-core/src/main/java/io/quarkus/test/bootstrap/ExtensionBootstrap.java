package io.quarkus.test.bootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface ExtensionBootstrap {

    boolean appliesFor(ExtensionContext context);

    default void beforeAll(ExtensionContext context) {

    }

    default void afterAll(ExtensionContext context) {

    }

    default void updateServiceContext(ServiceContext context) {

    }

}
