package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.inject.KubectlFacade;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.FileUtils;

public class KubernetesQuarkusApplicationManagedResource implements QuarkusManagedResource {

    private static final String QUARKUS_KUBERNETES_TEMPLATE = "/quarkus-app-kubernetes-template.yml";
    private static final String QUARKUS_KUBERNETES_FILE = "kubernetes.yml";

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    private final QuarkusApplicationManagedResourceBuilder model;
    private final KubectlFacade facade;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;
    private String image;

    public KubernetesQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!init) {
            image = createImageAndPush();
            init = true;
        }

        String template = updateTemplate();
        loadKubernetesFile(template);

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
        return DockerUtils.createImageAndPush(model.getContext(), model.getLaunchMode(), model.getArtifact());
    }

    private void loadKubernetesFile(String template) {
        facade.apply(FileUtils.copyContentTo(model.getContext(), template, QUARKUS_KUBERNETES_FILE));
    }

    private String updateTemplate() {
        String template = FileUtils.loadFile(QUARKUS_KUBERNETES_TEMPLATE)
                .replaceAll(quote("${NAMESPACE}"), facade.getNamespace())
                .replaceAll(quote("${IMAGE}"), image)
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getName())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());

        template = facade.addPropertiesToDeployment(model.getContext().getOwner().getProperties(), template);

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
