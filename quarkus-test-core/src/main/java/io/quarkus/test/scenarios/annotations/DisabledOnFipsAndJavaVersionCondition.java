package io.quarkus.test.scenarios.annotations;

import static io.quarkus.test.utils.FipsUtils.isFipsEnabledEnvironment;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class DisabledOnFipsAndJavaVersionCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled(
            "@DisabledOnFipsAndJavaVersion is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        return AnnotationUtils.findAnnotation(element, DisabledOnFipsAndJavaVersion.class)
                .filter(this::isDisabledOnCurrentEnvironment)
                .map(this::testIsDisabled)
                .orElse(ENABLED_BY_DEFAULT);
    }

    private boolean isDisabledOnCurrentEnvironment(DisabledOnFipsAndJavaVersion disabledOnFips) {
        if (!isFipsEnabledEnvironment()) {
            return false;
        }

        int javaVersion = Runtime.version().feature();
        return javaVersion >= disabledOnFips.min() && javaVersion <= disabledOnFips.max();
    }

    private ConditionEvaluationResult testIsDisabled(DisabledOnFipsAndJavaVersion disabledOnFips) {
        return ConditionEvaluationResult.disabled("Disabled in FIPS enabled environment on Java "
                + Runtime.version().feature() + " (reason: " + disabledOnFips.reason() + ")");
    }
}
