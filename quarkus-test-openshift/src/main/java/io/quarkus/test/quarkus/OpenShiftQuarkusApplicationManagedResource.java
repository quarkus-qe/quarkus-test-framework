package io.quarkus.test.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.quarkus.test.ManagedResource;
import io.quarkus.test.NativeTest;
import io.quarkus.test.extension.OpenShiftExtensionBootstrap;
import io.quarkus.test.logging.Log;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;
import io.quarkus.test.openshift.OpenShiftFacade;
import io.quarkus.test.utils.Command;

public class OpenShiftQuarkusApplicationManagedResource implements ManagedResource {

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-template.yml";
    private static final String QUARKUS_OPENSHIFT_FILE = "openshift.yml";

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;
    private static final int EXTERNAL_PORT = 80;

    private final QuarkusApplicationManagedResourceBuilder model;
    private final OpenShiftFacade facade;

    private LoggingHandler loggingHandler;
    private boolean running;

    public OpenShiftQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
        this.facade = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        String template = updateTemplate();
        loadOpenShiftFile(template);
        awaitForImageStreams(template);
        startBuild();
        facade.exposeService(model.getContext().getOwner().getName(), getInternalPort());

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
        loggingHandler.startWatching();
        running = true;
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        facade.setReplicaTo(model.getContext().getOwner().getName(), 0);
        running = false;
    }

    @Override
    public String getHost() {
        return facade.getUrlFromRoute(model.getContext().getOwner().getName());
    }

    @Override
    public int getPort() {
        return EXTERNAL_PORT;
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    private void startBuild() {
        String fromArg = "--from-dir=" + model.getArtifact().toAbsolutePath().getParent().toString();
        if (isNativeTest()) {
            fromArg = "--from-file=" + model.getArtifact().toAbsolutePath().toString();
        }

        try {
            new Command("oc", "start-build", model.getContext().getOwner().getName(), fromArg, "--follow").runAndWait();
        } catch (Exception e) {
            fail("Failed when starting build. Caused by " + e.getMessage());
        }
    }

    private void loadOpenShiftFile(String template) {
        Path openShiftFile = this.model.getContext().getServiceFolder().resolve(QUARKUS_OPENSHIFT_FILE);

        try {
            Files.writeString(openShiftFile, template);
        } catch (IOException e) {
            fail("Failed when writing OpenShift file. Caused by " + e.getMessage());
        }

        facade.apply(openShiftFile);
    }

    private String updateTemplate() {
        String template = loadTemplate();
        QuarkusImage config = QuarkusImage.UBI_QUARKUS_JVM_S2I;
        if (isNativeTest()) {
            config = QuarkusImage.UBI_QUARKUS_NATIVE_S2I;
        }

        template = template.replaceAll(quote("${NAMESPACE}"), facade.getNamespace())
                .replaceAll(quote("${SERVICE_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER}"), config.get(model.getContext()))
                .replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER_VERSION}"), config.getVersion())
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${INTERNAL_PORT}"), "" + getInternalPort());

        template = addProperties(template);

        return template;
    }

    private String addProperties(String template) {
        if (!model.getContext().getOwner().getProperties().isEmpty()) {
            List<HasMetadata> objs = facade.loadYaml(template);
            for (HasMetadata obj : objs) {
                if (obj instanceof DeploymentConfig) {
                    DeploymentConfig dc = (DeploymentConfig) obj;
                    dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
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

    private void awaitForImageStreams(String template) {
        Log.info(model.getContext().getOwner(), "Waiting for image streams ... ");
        List<HasMetadata> objs = facade.loadYaml(template);
        for (HasMetadata obj : objs) {
            if (obj instanceof ImageStream
                    && !StringUtils.equals(obj.getMetadata().getName(), model.getContext().getOwner().getName())) {
                ImageStream is = (ImageStream) obj;
                Awaitility.await().atMost(5, TimeUnit.MINUTES).until(() -> facade.hasImageStreamTags(is));
            }
        }
    }

    private int getInternalPort() {
        String internalPort = model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY);
        if (StringUtils.isNotBlank(internalPort)) {
            return Integer.parseInt(internalPort);
        }

        return INTERNAL_PORT_DEFAULT;
    }

    private boolean isNativeTest() {
        return model.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(NativeTest.class);
    }

    private String loadTemplate() {
        try {
            return IOUtils.toString(
                    OpenShiftQuarkusApplicationManagedResource.class.getResourceAsStream(QUARKUS_OPENSHIFT_TEMPLATE),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not load OpenShift template. Caused by " + e.getMessage());
        }

        return null;
    }

}
