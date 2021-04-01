package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.utils.DockerUtils;

public class ContainerRegistryOpenShiftQuarkusApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-registry-openshift-template.yml";
    private static final String DEPLOYMENT = "openshift.yml";

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    private String image;

    public ContainerRegistryOpenShiftQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void doInit() {
        image = createImageAndPush();
        applyTemplate();
        client.rollout(model.getContext().getOwner());
        client.expose(model.getContext().getOwner(), getInternalPort());
    }

    @Override
    protected void doUpdate() {
        applyTemplate();
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), model.getLaunchMode(), model.getArtifact());
    }

    private void applyTemplate() {
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(), QUARKUS_OPENSHIFT_TEMPLATE,
                this::replaceDeploymentContent,
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String replaceDeploymentContent(String content) {
        return content.replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());
    }

    private int getInternalPort() {
        String internalPort = model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY);
        if (StringUtils.isNotBlank(internalPort)) {
            return Integer.parseInt(internalPort);
        }

        return INTERNAL_PORT_DEFAULT;
    }

}
