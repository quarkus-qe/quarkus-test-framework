package io.quarkus.test.tracing;

import static io.quarkus.test.tracing.QuarkusScenarioTags.ERROR;
import static io.quarkus.test.tracing.QuarkusScenarioTags.SUCCESS;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.Sets;

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
    private static final String DEFAULT_JAEGER_HTTP_ENDPOINT = "http://localhost:14268/api/traces";
    private final Tracer tracer;
    private final QuarkusScenarioSpan quarkusScenarioSpan;
    private final QuarkusScenarioTags quarkusScenarioTags;

    public QuarkusScenarioTracer() {

        String serviceName = new PropertyLookup("ts.service-name", DEFAULT_SERVICE_NAME).get();
        String jaegerHttpEndpoint = new PropertyLookup("ts.jaeger-http-endpoint", DEFAULT_JAEGER_HTTP_ENDPOINT).get();

        tracer = new JaegerTracer.Builder(serviceName).withReporter(new RemoteReporter.Builder()
                .withSender(new HttpSender.Builder(jaegerHttpEndpoint).build()).build())
                .withSampler(new ConstSampler(true))
                .build();

        this.quarkusScenarioTags = new QuarkusScenarioTags();
        this.quarkusScenarioSpan = new QuarkusScenarioSpan(tracer, quarkusScenarioTags);
    }

    public void finishWithSuccess(ExtensionContext extensionContext) {
        finishWithSuccess(extensionContext, Sets.newHashSet(SUCCESS));
    }

    public void finishWithSuccess(ExtensionContext extensionContext, Set<String> tags) {
        tags.add(SUCCESS);
        quarkusScenarioSpan.save(Collections.emptyMap(), tags, extensionContext).finish();
    }

    public void finishWithError(ExtensionContext extensionContext, Throwable cause) {
        finishWithError(extensionContext, cause, Sets.newHashSet(ERROR));
    }

    public void finishWithError(ExtensionContext extensionContext, Throwable cause, Set<String> tags) {
        tags.add(ERROR);
        Map<String, ?> err = Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, cause, Fields.MESSAGE, cause.getMessage());
        quarkusScenarioSpan.save(err, tags, extensionContext).finish();
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
}
