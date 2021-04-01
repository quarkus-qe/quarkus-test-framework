package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class CheckIfSystemPropertyCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String systemProperty = getSystemPropertyName(context);
        String actual = System.getProperty(systemProperty);

        if (checkEnableCondition(context, actual)) {
            return ConditionEvaluationResult
                    .enabled(String.format("System property [%s] matches the condition", systemProperty));
        }

        return ConditionEvaluationResult
                .disabled(String.format("System property [%s] does not apply condition", systemProperty));
    }

    protected abstract boolean checkEnableCondition(ExtensionContext context, String actual);

    protected abstract String getSystemPropertyName(ExtensionContext context);
}
