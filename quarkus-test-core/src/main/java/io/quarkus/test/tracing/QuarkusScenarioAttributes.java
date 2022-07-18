package io.quarkus.test.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.trace.Span;
import io.quarkus.test.utils.TestExecutionProperties;

public class QuarkusScenarioAttributes {
    public static final String SUCCESS = "success";
    private final Map<String, String> globalAttributes = new HashMap<>();

    public QuarkusScenarioAttributes() {
        globalAttributes.put("buildNumber", TestExecutionProperties.getBuildNumber());
        globalAttributes.put("versionNumber", TestExecutionProperties.getVersionNumber());

        addPlatformAttributes();
    }

    public void initializedAttributes(Span span, Set<String> contextAttributes) {
        contextAttributes.forEach(attribute -> span.setAttribute(attribute, true));
        globalAttributes.forEach(span::setAttribute);
    }

    private void addPlatformAttributes() {
        if (TestExecutionProperties.isOpenshiftPlatform()) {
            globalAttributes.put("ocp", "true");
        }

        if (TestExecutionProperties.isKubernetesPlatform()) {
            globalAttributes.put("k8s", "true");
        }

        if (TestExecutionProperties.isBareMetalPlatform()) {
            globalAttributes.put("bare-metal", "true");
        }
    }
}
