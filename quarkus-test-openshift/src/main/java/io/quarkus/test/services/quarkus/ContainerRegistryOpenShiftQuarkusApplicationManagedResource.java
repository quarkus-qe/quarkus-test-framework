package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
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
        exposeServices();
    }

    protected String replaceDeploymentContent(String content) {
        return content.replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString());
    }

    private void exposeServices() {
        client.expose(model.getContext().getOwner(), HTTP_PORT_DEFAULT);
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), model.getLaunchMode(), model.getArtifact());
    }
}
