package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static java.util.regex.Pattern.quote;

import io.quarkus.test.bootstrap.Service;
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
        exposeServices();
    }

    protected String replaceDeploymentContent(String content) {
        return content.replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString());
    }

    private void exposeServices() {
        Service service = model.getContext().getOwner();
        client.expose(service, HTTP_PORT_DEFAULT);
        if (model.useSeparateManagementInterface()) {
            client.expose(model.getContext().getOwner().getName() + "-management", model.getManagementPort());
        }
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), getLaunchMode(), model.getArtifact());
    }
}
