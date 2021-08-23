package io.quarkus.test.tracing;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.quarkus.test.bootstrap.ScenarioContext;

public class QuarkusScenarioSpan {

    private final Tracer tracer;
    private final QuarkusScenarioTags quarkusScenarioTags;
    private final Map<String, Span> spanBucket = new ConcurrentHashMap<>();

    public QuarkusScenarioSpan(Tracer tracer, QuarkusScenarioTags quarkusScenarioTags) {
        this.quarkusScenarioTags = quarkusScenarioTags;
        this.tracer = tracer;
    }

    public Span getOrCreate(ScenarioContext context) {
        String operationName = getOperationName(context);
        if (!spanBucket.containsKey(operationName)) {
            return buildNewSpan(operationName, context.getTestContext().getTags());
        }

        return spanBucket.get(operationName);
    }

    public Span save(Map<String, ?> logs, Set<String> tags, ScenarioContext scenarioContext) {
        String operationName = getOperationName(scenarioContext);
        Span span = spanBucket.get(operationName);
        span.log(logs);
        tags.forEach(tag -> span.setTag(tag, true));
        return span;
    }

    public String getOperationName(ScenarioContext scenarioContext) {
        String operationName = scenarioContext.getRunningTestClassName();
        Optional<String> methodName = scenarioContext.getRunningTestMethodName();
        if (methodName.isPresent()) {
            operationName += "_" + methodName.get();
        }

        return operationName;
    }

    private Span buildNewSpan(String operationName, Set<String> tags) {
        Span span = tracer.buildSpan(operationName).start();
        quarkusScenarioTags.initializedTags(span, tags);
        spanBucket.put(operationName, span);
        return span;
    }
}
