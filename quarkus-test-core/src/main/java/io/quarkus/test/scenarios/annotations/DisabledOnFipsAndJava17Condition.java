package io.quarkus.test.scenarios.annotations;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnFipsAndJava17Condition implements ExecutionCondition {

    /**
     * We set environment variable "FIPS" to "fips" in our Jenkins jobs when FIPS are enabled.
     */
    private static final String FIPS_ENABLED = "fips";
    private static final int JAVA_17 = 17;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isFipsEnabledEnvironment() && isJava17()) {
            return ConditionEvaluationResult.disabled("The test is running in FIPS enabled environment with Java 17");
        }

        return ConditionEvaluationResult.enabled("The test is not running in FIPS enabled environment with Java 17");
    }

    private static boolean isFipsEnabledEnvironment() {
        return FIPS_ENABLED.equals(System.getenv().get("FIPS"));
    }

    private static boolean isJava17() {
        return JAVA_17 == Runtime.version().feature();
    }
}
