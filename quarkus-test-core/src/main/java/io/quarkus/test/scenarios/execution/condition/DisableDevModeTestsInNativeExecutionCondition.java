package io.quarkus.test.scenarios.execution.condition;

import static io.quarkus.test.scenarios.execution.condition.AbstractQuarkusScenarioContainerExecutionCondition.CONDITION_NOT_MATCHED;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.isNativeEnabled;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.services.RemoteDevModeQuarkusApplication;

public class DisableDevModeTestsInNativeExecutionCondition implements QuarkusScenarioExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context
                .getElement()
                .filter(AbstractQuarkusScenarioContainerExecutionCondition::isQuarkusScenario)
                .map(clazz -> (Class<?>) clazz)
                .map(DisableDevModeTestsInNativeExecutionCondition::evaluate)
                .orElse(CONDITION_NOT_MATCHED);
    }

    private static ConditionEvaluationResult evaluate(Class<?> testClass) {
        if (isNativeEnabled() && isDevModeTest(testClass)) {
            return ConditionEvaluationResult.disabled("DEV mode tests can't be run when native mode is enabled");
        } else {
            return ConditionEvaluationResult.enabled("Not a DEV mode test in native mode");
        }
    }

    private static boolean isDevModeTest(Class<?> testClass) {
        return Arrays.stream(testClass.getDeclaredFields())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .anyMatch(m -> m.isAnnotationPresent(RemoteDevModeQuarkusApplication.class)
                        || m.isAnnotationPresent(DevModeQuarkusApplication.class));
    }
}
