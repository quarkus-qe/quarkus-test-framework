package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.inject.KubectlFacade;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.DockerUtils;

public class KubernetesQuarkusApplicationManagedResource implements ManagedResource {

    private static final String DOCKERFILE_TEMPLATE = "/Dockerfile.%s";
    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-app-kubernetes-template.yml";
    private static final String QUARKUS_OPENSHIFT_FILE = "kubernetes.yml";
    private static final String DOCKERFILE = "Dockerfile";
    private static final PropertyLookup CONTAINER_REGISTY_URL = new PropertyLookup("ts.container.registry-url");

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    private final QuarkusApplicationManagedResourceBuilder model;
    private final KubectlFacade facade;
    private final String containerRegistryUrl;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    public KubernetesQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
        this.containerRegistryUrl = CONTAINER_REGISTY_URL.get(model.getContext());
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!init) {
            String image = createImageAndPush();
            String template = updateTemplate(image);
            loadOpenShiftFile(template);

            init = true;
        }

        facade.setReplicaTo(model.getContext().getName(), 1);
        running = true;

        loggingHandler = new KubernetesLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        facade.setReplicaTo(model.getContext().getName(), 0);
        running = false;
    }

    @Override
    public String getHost() {
        return facade.getUrlByService(model.getContext().getName());
    }

    @Override
    public int getPort() {
        return facade.getPortByService(model.getContext().getName());
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    private String createImageAndPush() {
        String dockerfileContent = loadFile(String.format(DOCKERFILE_TEMPLATE, model.getLaunchMode().name()))
                .replaceAll(quote("${ARTIFACT_PARENT}"), model.getArtifact().getParent().toString());

        Path dockerfilePath = copyContentTo(dockerfileContent, DOCKERFILE);
        DockerUtils.buildService(dockerfilePath, model.getContext());
        return DockerUtils.pushToContainerRegistryUrl(model.getContext(), this.containerRegistryUrl);
    }

    private void loadOpenShiftFile(String template) {
        facade.apply(copyContentTo(template, QUARKUS_OPENSHIFT_FILE));
    }

    private String updateTemplate(String image) {
        String template = loadFile(QUARKUS_OPENSHIFT_TEMPLATE).replaceAll(quote("${NAMESPACE}"), facade.getNamespace())
                .replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());

        template = addProperties(template);

        return template;
    }

    private String addProperties(String template) {
        if (!model.getContext().getOwner().getProperties().isEmpty()) {
            List<HasMetadata> objs = facade.loadYaml(template);
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

    private Path copyContentTo(String content, String target) {
        Path file = this.model.getContext().getServiceFolder().resolve(target);

        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            fail("Failed when writing file " + target + ". Caused by " + e.getMessage());
        }

        return file;
    }

    private String loadFile(String file) {
        try {
            return IOUtils.toString(
                    KubernetesQuarkusApplicationManagedResource.class.getResourceAsStream(file),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not load file " + file + " . Caused by " + e.getMessage());
        }

        return null;
    }

}
