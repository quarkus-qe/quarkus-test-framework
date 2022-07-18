package io.quarkus.test.tracing;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.test.bootstrap.ScenarioContext;

public class QuarkusScenarioSpan {

    private final QuarkusScenarioAttributes quarkusScenarioAttributes;
    private final Map<String, Span> spanBucket = new ConcurrentHashMap<>();

    private final Tracer tracer;

    public QuarkusScenarioSpan(Tracer tracer, QuarkusScenarioAttributes quarkusScenarioAttributes) {
        this.quarkusScenarioAttributes = quarkusScenarioAttributes;
        this.tracer = tracer;
    }

    public Span getOrCreate(ScenarioContext context) {
        String operationName = getOperationName(context);
        if (!spanBucket.containsKey(operationName)) {
            return buildNewSpan(operationName, context.getTestContext().getTags());
        }

        return spanBucket.get(operationName);
    }

    public Span save(Map<String, Boolean> attributes, ScenarioContext scenarioContext, Throwable throwable) {
        final Span span = spanBucket.get(getOperationName(scenarioContext));
        attributes.forEach(span::setAttribute);
        if (throwable == null) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR);
            span.recordException(throwable);
        }
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

    private Span buildNewSpan(String operationName, Set<String> attributes) {
        Span span = tracer.spanBuilder(operationName).setSpanKind(SpanKind.INTERNAL).startSpan();
        quarkusScenarioAttributes.initializedAttributes(span, attributes);
        spanBucket.put(operationName, span);
        return span;
    }
}
