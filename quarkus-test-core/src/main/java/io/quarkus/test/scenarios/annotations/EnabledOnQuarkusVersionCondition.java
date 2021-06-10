package io.quarkus.test.scenarios.annotations;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class EnabledOnQuarkusVersionCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled(
            "@EnabledOnQuarkusVersion is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        return AnnotationUtils.findRepeatableAnnotations(element, EnabledOnQuarkusVersion.class)
                .stream()
                .filter(this::isDisabledOnCurrentQuarkusVersion)
                .findAny()
                .map(this::testIsDisabled)
                .orElse(ENABLED_BY_DEFAULT);
    }

    private ConditionEvaluationResult testIsDisabled(EnabledOnQuarkusVersion disabledOnQuarkus) {
        return ConditionEvaluationResult.disabled("Disabled on Quarkus version (reason: " + disabledOnQuarkus.reason() + ")");
    }

    private boolean isDisabledOnCurrentQuarkusVersion(EnabledOnQuarkusVersion enabledOnQuarkus) {
        Pattern pattern = Pattern.compile(enabledOnQuarkus.version());
        String quarkusVersion = io.quarkus.builder.Version.getVersion();

        return !pattern.matcher(quarkusVersion).matches();
    }
}
