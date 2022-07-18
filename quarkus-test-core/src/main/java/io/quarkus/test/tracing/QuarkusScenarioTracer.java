package io.quarkus.test.tracing;

import static io.quarkus.test.tracing.QuarkusScenarioAttributes.SUCCESS;

import java.util.Map;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.utils.TestExecutionProperties;

public class QuarkusScenarioTracer {

    private static final String SERVICE_NAME = "service.name";
    private final QuarkusScenarioSpan quarkusScenarioSpan;
    private final SdkTracerProvider tracerProvider;

    public QuarkusScenarioTracer(String jaegerHttpEndpoint) {

        final var serviceName = TestExecutionProperties.getServiceName();
        tracerProvider = SdkTracerProvider
                .builder()
                .setResource(Resource.getDefault().toBuilder().put(SERVICE_NAME, serviceName).build())
                .addSpanProcessor(BatchSpanProcessor
                        .builder(OtlpGrpcSpanExporter.builder().setEndpoint(jaegerHttpEndpoint).build())
                        .build())
                .setSampler(Sampler.alwaysOn())
                .build();
        quarkusScenarioSpan = new QuarkusScenarioSpan(tracerProvider.get(serviceName), new QuarkusScenarioAttributes());
    }

    public void updateWithAttribute(ScenarioContext context, String attribute) {
        quarkusScenarioSpan.save(attributeToTrue(attribute), context, null);
    }

    public void finishWithSuccess(ScenarioContext context) {
        finishWithSuccess(context, SUCCESS);
    }

    public void finishWithSuccess(ScenarioContext context, String attribute) {
        quarkusScenarioSpan.save(attributeToTrue(attribute), context, null).end();
    }

    public void finishWithError(ScenarioContext context, Throwable cause) {
        quarkusScenarioSpan.save(Map.of(SUCCESS, false), context, cause).end();
    }

    public void finishWithError(ScenarioContext context, Throwable cause, String attribute) {
        quarkusScenarioSpan.save(Map.of(SUCCESS, false, attribute, true), context, cause).end();
    }

    public void createSpanContext(ScenarioContext context) {
        quarkusScenarioSpan.getOrCreate(context);
    }

    public void onShutdown() {
        tracerProvider.close();
    }

    private static Map<String, Boolean> attributeToTrue(String attribute) {
        return Map.of(attribute, true);
    }
}
