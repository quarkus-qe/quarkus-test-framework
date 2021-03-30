package io.quarkus.test.bootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

public interface ExtensionBootstrap {

    boolean appliesFor(ExtensionContext context);

    default void beforeAll(ExtensionContext context) {

    }

    default void afterAll(ExtensionContext context) {

    }

    default void updateServiceContext(ServiceContext context) {

    }

    default boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return false;
    }

    default Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return null;
    }

}
