package io.quarkus.test.scenarios.annotations;

import static io.quarkus.test.utils.FipsUtils.isFipsEnabledEnvironment;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnFipsCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isFipsEnabledEnvironment()) {
            return ConditionEvaluationResult.disabled("The test is running in FIPS enabled environment");
        }

        return ConditionEvaluationResult.enabled("The test is not running in FIPS enabled environment");
    }

}
