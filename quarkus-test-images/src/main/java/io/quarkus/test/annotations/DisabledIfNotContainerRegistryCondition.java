package io.quarkus.test.annotations;

import static io.quarkus.test.configuration.Configuration.Property.CONTAINER_REGISTRY_URL;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.scenarios.annotations.CheckIfSystemPropertyCondition;

public class DisabledIfNotContainerRegistryCondition extends CheckIfSystemPropertyCondition {

    @Override
    protected String getSystemPropertyName(ExtensionContext context) {
        return CONTAINER_REGISTRY_URL.getGlobalScopeName();
    }

    @Override
    protected boolean checkEnableCondition(ExtensionContext context, String actual) {
        return !StringUtils.isEmpty(actual);
    }
}
