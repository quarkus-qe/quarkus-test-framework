package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnSemeruJdkCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String javaRuntimeName = System.getProperties().getProperty("java.runtime.name");
        if (javaRuntimeName.toLowerCase().contains("semeru")) {
            return ConditionEvaluationResult.disabled("Running on semeru JDK");
        }
        return ConditionEvaluationResult.enabled("Running on non-semeru JDK");
    }
}
