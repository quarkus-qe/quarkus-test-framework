package io.quarkus.test.bootstrap;

import java.util.Optional;

import io.quarkus.test.scenarios.QuarkusScenario;

public class QuarkusCliExtensionBootstrap implements ExtensionBootstrap {

    private QuarkusCliClient client;

    @Override
    public boolean appliesFor(ScenarioContext context) {
        return context.isAnnotationPresent(QuarkusScenario.class);
    }

    @Override
    public void beforeAll(ScenarioContext context) {
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
