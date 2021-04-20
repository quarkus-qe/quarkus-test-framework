package io.quarkus.test.services.containers;

public class AmqOpenShiftContainerManagedResource extends OpenShiftContainerManagedResource {

    private static final String DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT = "/amq-openshift-deployment-template.yml";

    private final ContainerManagedResourceBuilder model;

    protected AmqOpenShiftContainerManagedResource(ContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    protected boolean useInternalServiceByDefault() {
        return true;
    }

    @Override
    protected String getTemplateByDefault() {
        return DEPLOYMENT_TEMPLATE_PROPERTY_DEFAULT;
    }
}
