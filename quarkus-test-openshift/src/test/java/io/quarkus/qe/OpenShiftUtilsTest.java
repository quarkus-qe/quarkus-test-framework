package io.quarkus.qe;

import static io.quarkus.test.bootstrap.inject.OpenShiftUtils.sanitizeLabelValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OpenShiftUtilsTest {

    @Test
    public void testSanitizeLabelValue() {
        var sanitizedValue = sanitizeLabelValue("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT-");
        assertEquals("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT", sanitizedValue);
        sanitizedValue = sanitizeLabelValue("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT_");
        assertEquals("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT", sanitizedValue);
        sanitizedValue = sanitizeLabelValue("+OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT_");
        assertEquals("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT", sanitizedValue);
        sanitizedValue = sanitizeLabelValue("@#$%^^+OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT");
        assertEquals("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT", sanitizedValue);
        sanitizedValue = sanitizeLabelValue("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT(&&^%");
        assertEquals("OpenShiftServerlessUsingExtensionDockerBuildStrategyVertxIT", sanitizedValue);
        sanitizedValue = sanitizeLabelValue("OpenShiftServerless@Using$ExtensionDockerBuildStrategyVertxIT");
        assertEquals("OpenShiftServerless_Using_ExtensionDockerBuildStrategyVertxIT", sanitizedValue);
    }

}
