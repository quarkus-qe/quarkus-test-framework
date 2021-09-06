package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.QUARKUS_HTTP_PORT_PROPERTY;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.DockerUtils;

public class KubernetesQuarkusApplicationManagedResource extends QuarkusManagedResource {

    private static final String DEPLOYMENT_SERVICE_PROPERTY = "kubernetes.service";
    private static final String DEPLOYMENT_TEMPLATE_PROPERTY = "kubernetes.template";
    private static final String QUARKUS_KUBERNETES_TEMPLATE = "/quarkus-app-kubernetes-template.yml";
    private static final String DEPLOYMENT = "kubernetes.yml";

    private final ArtifactQuarkusApplicationManagedResourceBuilder model;
    private final KubectlClient client;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;
    private String image;

    public KubernetesQuarkusApplicationManagedResource(ArtifactQuarkusApplicationManagedResourceBuilder model) {
        super(model.getContext());
        this.model = model;
        this.client = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
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

        loadDeployment();

        client.scaleTo(model.getContext().getOwner(), 1);
        running = true;

        loggingHandler = new KubernetesLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        client.scaleTo(model.getContext().getOwner(), 0);
        running = false;
    }

    @Override
    public String getHost(Protocol protocol) {
        validateProtocol(protocol);
        return client.url(model.getContext().getOwner());
    }

    @Override
    public int getPort(Protocol protocol) {
        validateProtocol(protocol);
        return client.port(model.getContext().getOwner());
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        if (model.containsBuildProperties()) {
            init = false;
            model.build();
        }

        start();
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    protected Map<String, String> addExtraTemplateProperties() {
        return Collections.emptyMap();
    }

    private void validateProtocol(Protocol protocol) {
        if (protocol == Protocol.HTTPS) {
            fail("SSL is not supported for Kubernetes tests yet");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for Kubernetes tests yet");
        }
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
