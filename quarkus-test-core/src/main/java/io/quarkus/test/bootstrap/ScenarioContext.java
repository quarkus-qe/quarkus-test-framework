package io.quarkus.test.bootstrap;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ScenarioContext {

    private static final int SCENARIO_ID_MAX_SIZE = 60;

    private final ExtensionContext testContext;
    private final String id;
    private final ExtensionContext.Namespace testNamespace;
    private ExtensionContext methodTestContext;

    protected ScenarioContext(ExtensionContext testContext) {
        this.testContext = testContext;
        this.id = generateScenarioId(testContext);
        this.testNamespace = ExtensionContext.Namespace.create(ScenarioContext.class);
    }

    public String getId() {
        return id;
    }

    public String getRunningTestClassName() {
        return getTestContext().getRequiredTestClass().getSimpleName();
    }

    public Optional<String> getRunningTestMethodName() {
        if (methodTestContext == null) {
            return Optional.empty();
        }

        return Optional.of(methodTestContext.getRequiredTestMethod().getName());
    }

    public ExtensionContext.Store getTestStore() {
        return getTestContext().getStore(this.testNamespace);
    }

    public ExtensionContext getTestContext() {
        return Optional.ofNullable(methodTestContext).orElse(testContext);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getTestContext().getRequiredTestClass().isAnnotationPresent(annotationClass);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getTestContext().getRequiredTestClass().getAnnotation(annotationClass);
    }

    public void setMethodTestContext(ExtensionContext methodTestContext) {
        this.methodTestContext = methodTestContext;
    }

    private static String generateScenarioId(ExtensionContext context) {
        String fullId = context.getRequiredTestClass().getSimpleName() + "-" + System.currentTimeMillis();
        return fullId.substring(0, Math.min(SCENARIO_ID_MAX_SIZE, fullId.length()));
    }
}
