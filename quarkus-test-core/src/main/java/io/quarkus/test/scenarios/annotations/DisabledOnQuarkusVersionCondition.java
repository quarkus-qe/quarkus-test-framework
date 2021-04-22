package io.quarkus.test.scenarios.annotations;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnQuarkusVersionCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled(
            "@DisabledOnQuarkusVersion is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnQuarkusVersion> disabledOnQuarkus = findAnnotation(element, DisabledOnQuarkusVersion.class);
        if (disabledOnQuarkus.isPresent()) {
            Pattern pattern = Pattern.compile(disabledOnQuarkus.get().version());
            String quarkusVersion = io.quarkus.builder.Version.getVersion();

            return pattern.matcher(quarkusVersion).matches()
                    ? ConditionEvaluationResult.disabled("Disabled on Quarkus version " + quarkusVersion + " (reason: "
                            + disabledOnQuarkus.get().reason() + ")")
                    : ConditionEvaluationResult.enabled("Enabled on Quarkus version " + quarkusVersion);
        }
        return ENABLED_BY_DEFAULT;
    }
}
