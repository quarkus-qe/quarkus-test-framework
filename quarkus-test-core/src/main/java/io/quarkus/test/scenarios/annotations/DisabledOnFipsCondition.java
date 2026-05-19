package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnFipsCondition implements ExecutionCondition {

    /**
     * We set environment variable "FIPS" to "fips" in our Jenkins jobs when FIPS are enabled.
     */
    private static final String FIPS_ENABLED = "fips";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isFipsEnabledEnvironment()) {
            return ConditionEvaluationResult.disabled("The test is running in FIPS enabled environment");
        }

        return ConditionEvaluationResult.enabled("The test is not running in FIPS enabled environment");
    }

    private static boolean isFipsEnabledEnvironment() {
        return FIPS_ENABLED.equalsIgnoreCase(System.getenv().get("FIPS"));
    }

}
