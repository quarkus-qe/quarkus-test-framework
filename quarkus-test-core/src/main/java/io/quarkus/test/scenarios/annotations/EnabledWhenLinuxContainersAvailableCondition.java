package io.quarkus.test.scenarios.annotations;

import static io.quarkus.test.scenarios.execution.condition.AbstractQuarkusScenarioContainerExecutionCondition.ENV_DOES_NOT_SUPPORT_LINUX_CONTAINERS;
import static io.quarkus.test.scenarios.execution.condition.AbstractQuarkusScenarioContainerExecutionCondition.ENV_SUPPORTS_LINUX_CONTAINERS;
import static java.lang.String.format;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.scenarios.execution.condition.AbstractQuarkusScenarioContainerExecutionCondition;

public class EnabledWhenLinuxContainersAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        if (AbstractQuarkusScenarioContainerExecutionCondition.areLinuxContainersSupported()) {
            return ENV_SUPPORTS_LINUX_CONTAINERS;
        } else {
            String testName = extensionContext.getDisplayName();
            return disabled(format(ENV_DOES_NOT_SUPPORT_LINUX_CONTAINERS, testName));
        }
    }
}
