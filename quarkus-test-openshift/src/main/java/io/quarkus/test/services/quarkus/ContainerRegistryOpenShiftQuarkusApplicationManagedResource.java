package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import io.quarkus.test.utils.DockerUtils;

public class ContainerRegistryOpenShiftQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-registry-openshift-template.yml";

    private String image;

    public ContainerRegistryOpenShiftQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected String getDefaultTemplate() {
        return QUARKUS_OPENSHIFT_TEMPLATE;
    }

    @Override
    protected void doInit() {
        image = createImageAndPush();
        super.doInit();
        client.rollout(model.getContext().getOwner());
        client.expose(model.getContext().getOwner(), getInternalPort());
    }

    protected String replaceDeploymentContent(String content) {
        return content.replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString());
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), model.getLaunchMode(), model.getArtifact());
    }

}
