package io.quarkus.test.annotations;

import static io.quarkus.test.utils.DockerUtils.CONTAINER_REGISTY_URL_PROPERTY;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledIfNotContainerRegistryCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String actual = System.getProperty(CONTAINER_REGISTY_URL_PROPERTY);

        if (StringUtils.isEmpty(actual)) {
            return ConditionEvaluationResult
                    .disabled(String.format("System property [%s] does not exist", CONTAINER_REGISTY_URL_PROPERTY));
        }

        return ConditionEvaluationResult
                .enabled(String.format("System property [%s] exists", CONTAINER_REGISTY_URL_PROPERTY));
    }
}
