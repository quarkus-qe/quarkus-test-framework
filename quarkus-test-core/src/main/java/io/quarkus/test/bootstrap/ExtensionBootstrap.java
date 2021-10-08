package io.quarkus.test.bootstrap;

import java.util.Optional;

public interface ExtensionBootstrap {

    boolean appliesFor(ScenarioContext context);

    default void beforeAll(ScenarioContext context) {

    }

    default void afterAll(ScenarioContext context) {

    }

    default void beforeEach(ScenarioContext context) {

    }

    default void afterEach(ScenarioContext context) {

    }

    default void onSuccess(ScenarioContext context) {

    }

    default void onDisabled(ScenarioContext context, Optional<String> reason) {

    }

    default void onError(ScenarioContext context, Throwable throwable) {

    }

    default void onServiceLaunch(ScenarioContext context, Service service) {

    }

    default void updateServiceContext(ServiceContext context) {

    }

    default Optional<Object> getParameter(Class<?> clazz) {
        return Optional.empty();
    }
}
