package io.quarkus.test.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.quarkus.test.configuration.PropertyLookup;

public class QuarkusScenarioTags {
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    private static final String DEFAULT_BUILD_NUMBER = "777-default";
    private static final String DEFAULT_VERSION_NUMBER = "999-default";
    private final Map<String, String> globalTags = new HashMap<>();

    public QuarkusScenarioTags() {
        String quarkusPluginVersion = new PropertyLookup("quarkus-plugin.version", DEFAULT_VERSION_NUMBER).get();
        String buildNumber = new PropertyLookup("ts.buildNumber", DEFAULT_BUILD_NUMBER).get();
        String versionNumber = new PropertyLookup("ts.versionNumber", quarkusPluginVersion).get();
        String openshift = new PropertyLookup("openshift", "").get();
        String k8s = new PropertyLookup("kubernetes", "").get();

        globalTags.put("buildNumber", buildNumber);
        globalTags.put("versionNumber", versionNumber);

        addPlatformTag(openshift, k8s);
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

    private void addPlatformTag(String openshift, String k8s) {
        if (!openshift.isEmpty()) {
            globalTags.put("ocp", "true");
        }

        if (!k8s.isEmpty()) {
            globalTags.put("k8s", "true");
        }

        if (openshift.isEmpty() && k8s.isEmpty()) {
            globalTags.put("bare-metal", "true");
        }
    }
}
