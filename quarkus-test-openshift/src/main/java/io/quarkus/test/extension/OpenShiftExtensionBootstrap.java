package io.quarkus.test.extension;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.ServiceContext;
import io.quarkus.test.annotation.OpenShiftTest;
import io.quarkus.test.extensions.ExtensionBootstrap;
import io.quarkus.test.openshift.OpenShiftFacade;

public class OpenShiftExtensionBootstrap implements ExtensionBootstrap {

    public static final String CLIENT = "openshift-client";

    private OpenShiftFacade facade;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(OpenShiftTest.class);
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
        return ThreadLocalRandom.current().ints(10, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }




}
