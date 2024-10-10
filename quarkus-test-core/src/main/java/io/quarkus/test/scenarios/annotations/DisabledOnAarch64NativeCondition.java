package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class DisabledOnAarch64NativeCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        /*
         * This only triggers, if test executor is running on aarch64.
         * e.g. It doesn't work if test executor is amd64 and targeted OCP cluster is aarch.
         * But for native this should not be a problem, since native images has to be built on aarch and then exectued on aarch.
         */
        boolean isAarch64 = System.getProperty("os.arch").toLowerCase().trim().equals("aarch64");
        boolean isNative = QuarkusProperties.isNativeEnabled();

        if (isAarch64 && isNative) {
            return ConditionEvaluationResult.disabled("Skipping test as it's not running on aarch64 native");
        }

        return ConditionEvaluationResult.enabled("Running test as it's running on aarch64 native");
    }
}
