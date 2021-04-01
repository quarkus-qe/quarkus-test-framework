package io.quarkus.test.scenarios.annotations;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnabledIfOpenShiftScenarioPropertyIsTrueCondition extends CheckIfSystemPropertyCondition {

    private static final String SCENARIO_ENABLED_PROPERTY = "ts.openshift.scenario.enabled";

    @Override
    protected String getSystemPropertyName(ExtensionContext context) {
        return SCENARIO_ENABLED_PROPERTY;
    }

    @Override
    protected boolean checkEnableCondition(ExtensionContext context, String actual) {
        return StringUtils.equalsIgnoreCase(actual, Boolean.TRUE.toString());
    }
}
