package io.quarkus.test.tracing;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;

import io.opentracing.Span;
import io.opentracing.Tracer;

public class QuarkusScenarioSpan {

    private final Tracer tracer;
    private QuarkusScenarioTags quarkusScenarioTags;
    private final Map<String, Span> spanBucket = new ConcurrentHashMap<>();

    public QuarkusScenarioSpan(Tracer tracer, QuarkusScenarioTags quarkusScenarioTags) {
        this.quarkusScenarioTags = quarkusScenarioTags;
        this.tracer = tracer;
    }

    public Span getOrCreate(ExtensionContext extensionContext) {
        String spanId = getSpanId(extensionContext);
        if (!spanBucket.containsKey(spanId)) {
            return buildNewSpan(spanId, extensionContext.getTags());
        }

        return spanBucket.get(spanId);
    }

    public Span save(Map<String, ?> logs, Set<String> tags, ExtensionContext extensionContext) {
        String spanId = getSpanId(extensionContext);
        Span span = spanBucket.get(spanId);
        span.log(logs);
        tags.forEach(tag -> span.setTag(tag, true));
        return spanBucket.put(spanId, span);
    }

    public String getSpanId(ExtensionContext extensionContext) {
        String spanId = extensionContext.getRequiredTestClass().getSimpleName();
        try {
            spanId += "_" + extensionContext.getRequiredTestMethod().getName();
        } catch (PreconditionViolationException e) {
            // there is no method name to append
        }

        return spanId;
    }

    private Span buildNewSpan(String spanId, Set<String> tags) {
        Span span = tracer.buildSpan(spanId).start();
        quarkusScenarioTags.initializedTags(span, tags);
        spanBucket.put(spanId, span);
        return span;
    }
}
