package io.quarkus.test.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.quarkus.test.utils.TestExecutionProperties;

public class QuarkusScenarioTags {
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    private final Map<String, String> globalTags = new HashMap<>();

    public QuarkusScenarioTags() {
        globalTags.put("buildNumber", TestExecutionProperties.getBuildNumber());
        globalTags.put("versionNumber", TestExecutionProperties.getVersionNumber());

        addPlatformTag();
    }

    public void initializedTags(Span span, Set<String> contextTags) {
        contextTags.forEach(tag -> span.setTag(tag, true));
        globalTags.entrySet().forEach(gTag -> span.setTag(gTag.getKey(), gTag.getValue()));
    }

    public void setTag(Span span, String tagName) {
        span.setTag(tagName, true);
    }

    public void setErrorTag(Span span) {
        Tags.ERROR.set(span, true);
    }

    private void addPlatformTag() {
        if (TestExecutionProperties.isOpenshiftPlatform()) {
            globalTags.put("ocp", "true");
        }

        if (TestExecutionProperties.isKubernetesPlatform()) {
            globalTags.put("k8s", "true");
        }

        if (TestExecutionProperties.isBareMetalPlatform()) {
            globalTags.put("bare-metal", "true");
        }
    }
}
