package io.quarkus.test.bootstrap;

import java.util.Optional;

import io.quarkus.test.bootstrap.config.QuarkusConfigCommand;
import io.quarkus.test.bootstrap.tls.QuarkusTlsCommand;
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

        if (clazz == QuarkusConfigCommand.class) {
            // let's keep it @Dependent so that we have one app per a test class
            return Optional.of(new QuarkusConfigCommand(client));
        }

        if (clazz == QuarkusTlsCommand.class) {
            // let's keep it @Dependent so that we have one app per a test class
            return Optional.of(new QuarkusTlsCommand(client));
        }

        return Optional.empty();
    }
}
