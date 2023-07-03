package io.quarkus.test.scenarios.annotations;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Objects;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class DisabledOnNativeCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (QuarkusProperties.isNativePackageType()) {
            return disabled("It's running the test on Native, detected through package type");
        }
        String property = System.getProperty("profile.id");
        if (Objects.equals(property, "native")) {
            return disabled("It's running the test on Native, detected through build profile");
        }
        return enabled("It's not running the test on Native");
    }
}
