package io.quarkus.test.annotations;

import static io.quarkus.test.utils.DockerUtils.CONTAINER_REGISTY_URL_PROPERTY;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.scenarios.annotations.CheckIfSystemPropertyCondition;

public class DisabledIfNotContainerRegistryCondition extends CheckIfSystemPropertyCondition {

    @Override
    protected String getSystemPropertyName(ExtensionContext context) {
        return CONTAINER_REGISTY_URL_PROPERTY;
    }

    @Override
    protected boolean checkEnableCondition(ExtensionContext context, String actual) {
        return !StringUtils.isEmpty(actual);
    }
}
