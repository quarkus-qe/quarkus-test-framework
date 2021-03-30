package io.quarkus.test.bootstrap;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftExtensionBootstrap implements ExtensionBootstrap {

    public static final String CLIENT = "openshift-client";

    private static final int PROJECT_NAME_SIZE = 10;

    private OpenShiftClient client;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        client = OpenShiftClient.create(generateRandomProject());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        client.deleteProject();
    }

    @Override
    public void updateServiceContext(ServiceContext context) {
        context.put(CLIENT, client);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == OpenShiftClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return client;
    }

    private String generateRandomProject() {
        return ThreadLocalRandom.current().ints(PROJECT_NAME_SIZE, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
