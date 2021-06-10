package io.quarkus.test.bootstrap;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.scenarios.QuarkusScenario;

public class QuarkusCliExtensionBootstrap implements ExtensionBootstrap {

    private ExtensionContext context;
    private QuarkusCliClient client;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        this.context = context;
        return context.getRequiredTestClass().isAnnotationPresent(QuarkusScenario.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        this.client = new QuarkusCliClient(context);
    }

    @Override
    public Optional<Object> getParameter(Class<?> clazz) {
        if (clazz == QuarkusCliClient.class) {
            return Optional.of(client);
        }

        return Optional.empty();
    }
}
