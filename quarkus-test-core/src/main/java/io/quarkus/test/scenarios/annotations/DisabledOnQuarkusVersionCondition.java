package io.quarkus.test.scenarios.annotations;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class DisabledOnQuarkusVersionCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled(
            "@DisabledOnQuarkusVersion is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        return AnnotationUtils.findRepeatableAnnotations(element, DisabledOnQuarkusVersion.class)
                .stream()
                .filter(this::isDisabledOnCurrentQuarkusVersion)
                .findAny()
                .map(this::testIsDisabled)
                .orElse(ENABLED_BY_DEFAULT);
    }

    private ConditionEvaluationResult testIsDisabled(DisabledOnQuarkusVersion disabledOnQuarkus) {
        return ConditionEvaluationResult.disabled("Disabled on Quarkus version (reason: " + disabledOnQuarkus.reason() + ")");
    }

    private boolean isDisabledOnCurrentQuarkusVersion(DisabledOnQuarkusVersion disabledOnQuarkus) {
        Pattern pattern = Pattern.compile(disabledOnQuarkus.version());
        String quarkusVersion = QuarkusProperties.getVersion();

        return pattern.matcher(quarkusVersion).matches();
    }
}
