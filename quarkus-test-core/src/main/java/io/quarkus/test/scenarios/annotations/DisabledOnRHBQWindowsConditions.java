package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.smallrye.common.os.OS;

public class DisabledOnRHBQWindowsConditions implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        boolean isWindows = OS.current().equals(OS.WINDOWS);
        if (QuarkusProperties.isRHBQ() && isWindows) {
            return ConditionEvaluationResult.disabled("It is RHBQ on Windows");
        } else {
            return ConditionEvaluationResult.enabled("It is not RHBQ on Windows");
        }
    }
}
