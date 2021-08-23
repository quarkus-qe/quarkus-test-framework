package io.quarkus.test.tracing;

import static io.quarkus.test.tracing.QuarkusScenarioTags.ERROR;
import static io.quarkus.test.tracing.QuarkusScenarioTags.SUCCESS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.thrift.transport.TTransportException;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.configuration.PropertyLookup;

public class QuarkusScenarioTracer {

    private static final String DEFAULT_SERVICE_NAME = "quarkus-test-framework";

    private final Tracer tracer;
    private final QuarkusScenarioSpan quarkusScenarioSpan;
    private final QuarkusScenarioTags quarkusScenarioTags;

    public QuarkusScenarioTracer(String jaegerHttpEndpoint) throws TTransportException {
        String serviceName = new PropertyLookup("ts.service-name", DEFAULT_SERVICE_NAME).get();

        tracer = new JaegerTracer.Builder(serviceName).withReporter(new RemoteReporter.Builder()
                .withSender(new HttpSender.Builder(jaegerHttpEndpoint).build()).build())
                .withSampler(new ConstSampler(true))
                .build();

        this.quarkusScenarioTags = new QuarkusScenarioTags();
        this.quarkusScenarioSpan = new QuarkusScenarioSpan(tracer, quarkusScenarioTags);
    }

    public void updateWithTag(ScenarioContext context, String tag) {
        quarkusScenarioSpan.save(Collections.emptyMap(), newHashSet(tag), context);
    }

    public void finishWithSuccess(ScenarioContext context) {
        finishWithSuccess(context, SUCCESS);
    }

    public void finishWithSuccess(ScenarioContext context, String tag) {
        quarkusScenarioSpan.save(Collections.emptyMap(), newHashSet(tag), context).finish();
    }

    public void finishWithError(ScenarioContext context, Throwable cause) {
        finishWithError(context, cause, ERROR);
    }

    public void finishWithError(ScenarioContext context, Throwable cause, String tag) {
        Map<String, ?> err = Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, cause, Fields.MESSAGE, cause.getMessage());
        quarkusScenarioSpan.save(err, newHashSet(tag), context).finish();
    }

    public Span createSpanContext(ScenarioContext context) {
        return quarkusScenarioSpan.getOrCreate(context);
    }

    public Tracer getTracer() {
        return tracer;
    }

    public QuarkusScenarioTags getTestFrameworkTags() {
        return quarkusScenarioTags;
    }

    private static Set<String> newHashSet(String... values) {
        Set<String> set = new HashSet<>();
        Stream.of(values).forEach(set::add);
        return set;
    }
}
