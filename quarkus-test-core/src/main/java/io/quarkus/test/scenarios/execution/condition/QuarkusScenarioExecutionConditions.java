package io.quarkus.test.scenarios.execution.condition;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.ServiceLoader;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class QuarkusScenarioExecutionConditions implements ExecutionCondition {

    private static final ConditionEvaluationResult SUCCESS = enabled("All QuarkusScenario execution condition passed");
    private final ServiceLoader<QuarkusScenarioExecutionCondition> delegates = ServiceLoader
            .load(QuarkusScenarioExecutionCondition.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        for (ExecutionCondition delegate : delegates) {
            var result = delegate.evaluateExecutionCondition(extensionContext);
            if (result.isDisabled()) {
                return result;
            }
        }
        return SUCCESS;
    }

}
