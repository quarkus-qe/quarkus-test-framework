package io.quarkus.test.bootstrap;

import java.util.Optional;

import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.QuarkusScenario;

public class QuarkusHelmExtensionBootstrap implements ExtensionBootstrap {

    private ScenarioContext context;
    private QuarkusHelmClient client;

    @Override
    public boolean appliesFor(ScenarioContext context) {
        this.context = context;
        return context.isAnnotationPresent(QuarkusScenario.class)
                || context.isAnnotationPresent(OpenShiftScenario.class)
                || context.isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public void beforeAll(ScenarioContext context) {
        this.client = new QuarkusHelmClient(context);
    }

    @Override
    public Optional<Object> getParameter(Class<?> clazz) {
        if (clazz == QuarkusHelmClient.class) {
            return Optional.of(client);
        }

        return Optional.empty();
    }
}
