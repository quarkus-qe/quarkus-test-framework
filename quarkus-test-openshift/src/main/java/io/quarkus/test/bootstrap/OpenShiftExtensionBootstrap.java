package io.quarkus.test.bootstrap;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.bootstrap.inject.OpenShiftFacade;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftExtensionBootstrap implements ExtensionBootstrap {

    public static final String CLIENT = "openshift-client";

    private static final int PROJECT_NAME_SIZE = 10;

    private OpenShiftFacade facade;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        facade = OpenShiftFacade.create(generateRandomProject());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        facade.deleteProject();
    }

    @Override
    public void updateServiceContext(ServiceContext context) {
        context.put(CLIENT, facade);
    }

    private String generateRandomProject() {
        return ThreadLocalRandom.current().ints(PROJECT_NAME_SIZE, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
