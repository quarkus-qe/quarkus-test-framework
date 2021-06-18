package io.quarkus.test.tracing;

import static io.quarkus.test.tracing.QuarkusScenarioTags.ERROR;
import static io.quarkus.test.tracing.QuarkusScenarioTags.SUCCESS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.quarkus.test.configuration.PropertyLookup;

public class QuarkusScenarioTracer {

    private static final String DEFAULT_SERVICE_NAME = "quarkus-test-framework";

    private final Tracer tracer;
    private final QuarkusScenarioSpan quarkusScenarioSpan;
    private final QuarkusScenarioTags quarkusScenarioTags;

    public QuarkusScenarioTracer(String jaegerHttpEndpoint) {
        String serviceName = new PropertyLookup("ts.service-name", DEFAULT_SERVICE_NAME).get();

        tracer = new JaegerTracer.Builder(serviceName).withReporter(new RemoteReporter.Builder()
                .withSender(new HttpSender.Builder(jaegerHttpEndpoint).build()).build())
                .withSampler(new ConstSampler(true))
                .build();

        this.quarkusScenarioTags = new QuarkusScenarioTags();
        this.quarkusScenarioSpan = new QuarkusScenarioSpan(tracer, quarkusScenarioTags);
    }

    public void finishWithSuccess(ExtensionContext extensionContext) {
        finishWithSuccess(extensionContext, SUCCESS);
    }

    public void finishWithSuccess(ExtensionContext extensionContext, String tag) {
        quarkusScenarioSpan.save(Collections.emptyMap(), newHashSet(tag), extensionContext).finish();
    }

    public void finishWithError(ExtensionContext extensionContext, Throwable cause) {
        finishWithError(extensionContext, cause, ERROR);
    }

    public void finishWithError(ExtensionContext extensionContext, Throwable cause, String tag) {
        Map<String, ?> err = Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, cause, Fields.MESSAGE, cause.getMessage());
        quarkusScenarioSpan.save(err, newHashSet(tag), extensionContext).finish();
    }

    public Span createSpanContext(ExtensionContext extensionContext) {
        return quarkusScenarioSpan.getOrCreate(extensionContext);
    }

    public Tracer getTracer() {
        return tracer;
    }

    public QuarkusScenarioTags getTestFrameworkTags() {
        return quarkusScenarioTags;
    }

    private static Set<String> newHashSet(String value) {
        Set<String> set = new HashSet<>();
        set.add(value);
        return set;
    }
}
