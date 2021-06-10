package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class EnabledOnNativeCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (QuarkusProperties.isNativePackageType()) {
            return ConditionEvaluationResult.enabled("Running test as it's running on Native");
        }

        return ConditionEvaluationResult.disabled("Skipping test as it's not running on Native");
    }
}
