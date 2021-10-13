package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;
import static java.util.regex.Pattern.quote;

public class OpenShiftJaegerContainerManagedResource extends OpenShiftContainerManagedResource {

    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/jaeger-deployment-template.yml";

    private static final String TRACE_SUFFIX = "-query";
    private static final String REST_SUFFIX = "-rest";

    private final JaegerContainerManagedResourceBuilder model;

    protected OpenShiftJaegerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    @Override
    protected String getTemplateByDefault() {
        return DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT;
    }

    @Override
    protected boolean useInternalServiceByDefault() {
        return true;
    }

    @Override
    protected String getInternalServiceName() {
        return model.getContext().getName() + REST_SUFFIX;
    }

    @Override
    protected void exposeService() {
        super.exposeService();
        // We need to expose an additional endpoint for trace
        String traceServiceName = model.getContext().getName() + TRACE_SUFFIX;
        getClient().expose(traceServiceName, model.getTracePort());
        model.getContext().put(JAEGER_TRACE_URL_PROPERTY, getClient().url(traceServiceName).toString());
    }

    @Override
    protected String replaceDeploymentContent(String content) {
        return super.replaceDeploymentContent(content)
                .replaceAll(quote("${TRACE_PORT}"), "" + model.getTracePort());
    }

}
