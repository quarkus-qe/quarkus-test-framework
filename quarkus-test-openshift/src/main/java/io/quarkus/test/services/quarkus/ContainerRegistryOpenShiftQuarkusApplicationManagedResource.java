package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.FileUtils;

public class ContainerRegistryOpenShiftQuarkusApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-registry-openshift-template.yml";
    private static final String QUARKUS_OPENSHIFT_FILE = "openshift.yml";

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    private String image;

    public ContainerRegistryOpenShiftQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void doInit() {
        image = createImageAndPush();
        String template = updateTemplate();
        loadOpenShiftFile(template);
        client.rollout(model.getContext().getOwner());
        client.expose(model.getContext().getOwner(), getInternalPort());
    }

    @Override
    protected void onRestart() {
        updateTemplate();
    }

    private String createImageAndPush() {
        return DockerUtils.createImageAndPush(model.getContext(), model.getLaunchMode(), model.getArtifact());
    }

    private void loadOpenShiftFile(String template) {
        client.apply(model.getContext().getOwner(),
                FileUtils.copyContentTo(template, model.getContext().getServiceFolder().resolve(QUARKUS_OPENSHIFT_FILE)));
    }

    private String updateTemplate() {
        String template = FileUtils.loadFile(QUARKUS_OPENSHIFT_TEMPLATE)
                .replaceAll(quote("${NAMESPACE}"), client.project())
                .replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());

        template = addProperties(template);

        return template;
    }

    private String addProperties(String template) {
        if (!model.getContext().getOwner().getProperties().isEmpty()) {
            List<HasMetadata> objs = client.loadYaml(template);
            for (HasMetadata obj : objs) {
                if (obj instanceof Deployment) {
                    Deployment d = (Deployment) obj;
                    d.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                        model.getContext().getOwner().getProperties().entrySet()
                                .forEach(
                                        envVar -> container.getEnv().add(new EnvVar(envVar.getKey(), envVar.getValue(), null)));
                    });
                }
            }

            KubernetesList list = new KubernetesList();
            list.setItems(objs);
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Serialization.yamlMapper().writeValue(os, list);
                template = new String(os.toByteArray());
            } catch (IOException e) {
                fail("Failed adding properties into OpenShift template. Caused by " + e.getMessage());
            }
        }
        return template;
    }

    private int getInternalPort() {
        String internalPort = model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY);
        if (StringUtils.isNotBlank(internalPort)) {
            return Integer.parseInt(internalPort);
        }

        return INTERNAL_PORT_DEFAULT;
    }

}
