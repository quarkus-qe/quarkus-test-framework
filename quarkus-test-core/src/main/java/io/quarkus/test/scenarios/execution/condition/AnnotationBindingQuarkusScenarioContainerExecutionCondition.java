package io.quarkus.test.scenarios.execution.condition;

import java.util.Arrays;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.AnnotationBinding;

/**
 * Recognizes whether {@link AnnotationBinding} is associated to the resource that requires Linux containers and
 * disables tests when not available.
 */
public class AnnotationBindingQuarkusScenarioContainerExecutionCondition
        extends AbstractQuarkusScenarioContainerExecutionCondition {

    private final ServiceLoader<AnnotationBinding> bindingsRegistry = ServiceLoader.load(AnnotationBinding.class);

    @Override
    protected boolean areContainersRequired(Class<?> testClass) {
        return bindingsRegistry
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(AnnotationBinding::requiresLinuxContainersOnBareMetal)
                .anyMatch(binding -> testClassHasFieldsAnnotatedWith(testClass, binding));
    }

    private static boolean testClassHasFieldsAnnotatedWith(Class<?> testClass, AnnotationBinding binding) {
        return testClass != null
                && (Arrays.stream(testClass.getDeclaredFields()).anyMatch(binding::isFor)
                        || testClassHasFieldsAnnotatedWith(testClass.getSuperclass(), binding));
    }
}
