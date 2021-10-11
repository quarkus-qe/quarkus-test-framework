package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.QUARKUS_HTTP_PORT_PROPERTY;
import static java.util.regex.Pattern.quote;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.utils.DockerUtils;

public class ContainerRegistryKubernetesQuarkusApplicationManagedResource
        extends KubernetesQuarkusApplicationManagedResource<ArtifactQuarkusApplicationManagedResourceBuilder> {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "kubernetes.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "kubernetes.template";
    private static final String QUARKUS_KUBERNETES_TEMPLATE = "/quarkus-app-kubernetes-template.yml";
    private static final String DEPLOYMENT = "kubernetes.yml";

    private String image;

    public ContainerRegistryKubernetesQuarkusApplicationManagedResource(
            ArtifactQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void doInit() {
        image = createImageAndPush();
        loadDeployment();
    }

    @Override
    protected void doUpdate() {
        loadDeployment();
    }

    protected Map<String, String> addExtraTemplateProperties() {
        return Collections.emptyMap();
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), getLaunchMode(), model.getArtifact());
    }

    private void loadDeployment() {
        String deploymentFile = model.getContext().getOwner().getConfiguration().getOrDefault(DEPLOYMENT_TEMPLATE_PROPERTY,
                QUARKUS_KUBERNETES_TEMPLATE);
        client.applyServiceProperties(model.getContext().getOwner(), deploymentFile,
                this::replaceDeploymentContent,
                addExtraTemplateProperties(),
                model.getContext().getServiceFolder().resolve(DEPLOYMENT));
    }

    private String replaceDeploymentContent(String content) {
        String customServiceName = model.getContext().getOwner().getConfiguration().get(DEPLOYMENT_SERVICE_PROPERTY);
        if (StringUtils.isNotEmpty(customServiceName)) {
            // replace it by the service owner name
            content = content.replaceAll(quote(customServiceName), model.getContext().getName());
        }

        return content
                .replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"),
                        model.getContext().getOwner().getProperty(QUARKUS_HTTP_PORT_PROPERTY, "" + HTTP_PORT_DEFAULT));
    }

}
