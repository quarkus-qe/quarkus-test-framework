package io.quarkus.test.bootstrap.inject;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import okhttp3.Response;

public final class OpenShiftClient {

    public static final String LABEL_TO_WATCH_FOR_LOGS = "tsLogWatch";

    private static final int TIMEOUT_MINUTES = 5;
    private static final String RESOURCE_PREFIX = "resource::";
    private static final String RESOURCE_MNT_FOLDER = "/resource";
    private static final int PROJECT_NAME_SIZE = 10;
    private static final int PROJECT_CREATION_RETRIES = 5;

    private static final String OC = "oc";

    private final String currentNamespace;
    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;

    private OpenShiftClient() {
        currentNamespace = createProject();

        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(currentNamespace).build();

        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(currentNamespace);
    }

    public static OpenShiftClient create() {
        return new OpenShiftClient();
    }

    /**
     * @return the current project
     */
    public String project() {
        return currentNamespace;
    }

    /**
     * Apply the file into OpenShift.
     *
     * @param file
     */
    public void apply(Service service, Path file) {
        try {
            new Command(OC, "apply", "-f", file.toAbsolutePath().toString(), "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " for " + service.getName() + " . Caused by "
                    + e.getMessage());
        }
    }

    /**
     * Update the file and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     *
     * @param file
     */
    public void applyServicePropertiesUsingTemplate(Service service, String file, UnaryOperator<String> update, Path target) {
        String content = FileUtils.loadFile(file);
        content = enrichTemplate(service, update.apply(content));
        apply(service, FileUtils.copyContentTo(content, target));
    }

    /**
     * Update the deployment config using the service properties.
     *
     * @param service
     */
    public void applyServicePropertiesUsingDeploymentConfig(Service service) {
        DeploymentConfig dc = client.deploymentConfigs().withName(service.getName()).get();
        Map<String, String> enrichProperties = enrichProperties(service.getProperties(), dc);

        dc.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
            enrichProperties.entrySet().forEach(
                    envVar -> container.getEnv().add(new EnvVar(envVar.getKey(), envVar.getValue(), null)));
        });

        client.deploymentConfigs().createOrReplace(dc);
    }

    /**
     * Start rollout of the service.
     *
     * @param service
     */
    public void rollout(Service service) {
        try {
            new Command(OC, "rollout", "latest", "dc/" + service.getName(), "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Deployment failed to be started. Caused by " + e.getMessage());
        }
    }

    /**
     * Expose the service and port defined.
     *
     * @param service
     * @param port
     */
    public void expose(Service service, int port) {
        Route route = client.routes().withName(service.getName()).get();
        if (route != null) {
            // already exposed.
            return;
        }

        try {
            new Command(OC, "expose", "svc/" + service.getName(), "--port=" + port, "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    /**
     * Scale the service to the replicas.
     *
     * @param service
     * @param replicas
     */
    public void scaleTo(Service service, int replicas) {
        try {
            new Command(OC, "scale", "dc/" + service.getName(), "--replicas=" + replicas, "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be scaled. Caused by " + e.getMessage());
        }
    }

    /**
     * readyReplicas retrieve the number of ready replicas to the given service.
     *
     * @param service
     * @return ready replicas amount
     */
    public int readyReplicas(Service service) {
        DeploymentConfig dc = client.deploymentConfigs().withName(service.getName()).get();
        return Optional.ofNullable(dc.getStatus().getReadyReplicas()).orElse(0);
    }

    /**
     * Get all the logs for all the pods.
     *
     * @return
     */
    public Map<String, String> logs() {
        Map<String, String> logs = new HashMap<>();
        for (Pod pod : client.pods().list().getItems()) {
            String podName = pod.getMetadata().getName();
            logs.put(podName, client.pods().withName(podName).getLog());
        }

        return logs;
    }

    /**
     * Get all the logs for all the pods within one service.
     *
     * @param service
     * @return
     */
    public Map<String, String> logs(Service service) {
        Map<String, String> logs = new HashMap<>();
        for (Pod pod : client.pods().withLabel(LABEL_TO_WATCH_FOR_LOGS, service.getName()).list().getItems()) {
            if (isPodRunning(pod)) {
                String podName = pod.getMetadata().getName();
                logs.put(podName, client.pods().withName(podName).getLog());
            }
        }

        return logs;
    }

    /**
     * Resolve the url by the service.
     *
     * @param service
     * @return
     */
    public String url(Service service) {
        Route route = client.routes().withName(service.getName()).get();
        if (route == null || route.getSpec() == null) {
            fail("Route for service " + service.getName() + " not found");
        }

        String protocol = route.getSpec().getTls() == null ? "http" : "https";
        String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
        return String.format("%s://%s%s", protocol, route.getSpec().getHost(), path);
    }

    /**
     * Await for the existence of some resources contained in the file template.
     * At the moment, ImageStream is only supported.
     *
     * @param service
     * @param file
     */
    public void awaitFor(Service service, Path file) {
        try {
            List<HasMetadata> objs = loadYaml(Files.readString(file));
            for (HasMetadata obj : objs) {
                if (obj instanceof ImageStream
                        && !StringUtils.equals(obj.getMetadata().getName(), service.getName())) {
                    ImageStream is = (ImageStream) obj;
                    Awaitility.await().atMost(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                            .until(() -> hasImageStreamTags(is));
                }
            }
        } catch (IOException e) {
            fail("Fail to load the file " + file + ". Caused by " + e.getMessage());
        }
    }

    public String execOnPod(String namespace, String podName, String containerId, String... input)
            throws InterruptedException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch execLatch = new CountDownLatch(1);

        try (ExecWatch execWatch = client.pods().inNamespace(namespace).withName(podName).inContainer(containerId)
                .writingOutput(out)
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {
                        // do nothing
                    }

                    @Override
                    public void onFailure(Throwable throwable, Response response) {
                        execLatch.countDown();
                    }

                    @Override
                    public void onClose(int i, String s) {
                        execLatch.countDown();
                    }
                })
                .exec(input)) {
            execLatch.await(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            return out.toString();
        }
    }

    /**
     * Delete the current project and all its resources.
     */
    public void deleteProject() {
        try {
            new Command(OC, "delete", "project", client.getNamespace()).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
    }

    private String enrichTemplate(Service service, String template) {
        List<HasMetadata> objs = loadYaml(template);
        for (HasMetadata obj : objs) {
            // set namespace
            obj.getMetadata().setNamespace(project());

            if (obj instanceof DeploymentConfig) {
                DeploymentConfig dc = (DeploymentConfig) obj;

                // set deployment name
                dc.getMetadata().setName(service.getName());

                // set metadata to template
                dc.getSpec().getTemplate().getMetadata().setNamespace(project());

                // add label for logs
                dc.getSpec().getTemplate().getMetadata().getLabels().put(LABEL_TO_WATCH_FOR_LOGS, service.getName());

                // add env var properties
                Map<String, String> enrichProperties = enrichProperties(service.getProperties(), dc);
                dc.getSpec().getTemplate().getSpec().getContainers()
                        .forEach(container -> enrichProperties.entrySet().forEach(
                                property -> {
                                    String key = property.getKey();
                                    EnvVar envVar = getEnvVarByKey(key, container);
                                    if (envVar == null) {
                                        container.getEnv().add(new EnvVar(key, property.getValue(), null));
                                    } else {
                                        envVar.setValue(property.getValue());
                                    }
                                }));
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

        return template;
    }

    private EnvVar getEnvVarByKey(String key, Container container) {
        return container.getEnv().stream().filter(env -> StringUtils.equals(key, env.getName())).findFirst().orElse(null);
    }

    private Map<String, String> enrichProperties(Map<String, String> properties, DeploymentConfig dc) {
        Map<String, String> output = new HashMap<>();
        for (Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();
            if (isResource(entry.getValue())) {
                String path = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                String fileName = path.substring(1); // remove first /
                String configMapName = normalizeConfigMapName(fileName);
                // Create Config Map with the content of the file
                client.configMaps().createOrReplace(new ConfigMapBuilder()
                        .withNewMetadata().withName(configMapName).endMetadata()
                        .addToData(fileName, FileUtils.loadFile(path)).build());

                // Add the volume to the above config map
                dc.getSpec().getTemplate().getSpec().getVolumes().add(new VolumeBuilder().withName(configMapName)
                        .withConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build()).build());

                // Configure all the containers to map the volume
                dc.getSpec().getTemplate().getSpec().getContainers()
                        .forEach(container -> container.getVolumeMounts()
                                .add(new VolumeMountBuilder().withName(configMapName).withReadOnly(true)
                                        .withMountPath(RESOURCE_MNT_FOLDER).build()));

                value = RESOURCE_MNT_FOLDER + path;
            }

            output.put(entry.getKey(), value);
        }

        return output;
    }

    private String normalizeConfigMapName(String name) {
        return name.replaceAll(Pattern.quote("."), "-");
    }

    private boolean isResource(String key) {
        return key.startsWith(RESOURCE_PREFIX);
    }

    private boolean hasImageStreamTags(ImageStream is) {
        return !masterClient.imageStreams().withName(is.getMetadata().getName()).get().getSpec().getTags().isEmpty();
    }

    private boolean isPodRunning(Pod pod) {
        return pod.getStatus().getPhase().equals("Running");
    }

    private String createProject() {
        boolean projectCreated = false;

        String namespace = generateRandomProjectName();
        int index = 0;
        while (index < PROJECT_CREATION_RETRIES) {
            if (doCreateProject(namespace)) {
                projectCreated = true;
                break;
            }

            namespace = generateRandomProjectName();
            index++;
        }

        if (!projectCreated) {
            fail("Project cannot be created. Review your OpenShift installation.");
        }

        return namespace;
    }

    private boolean doCreateProject(String projectName) {
        boolean created = false;
        try {
            new Command(OC, "new-project", projectName).runAndWait();
            created = true;
        } catch (Exception e) {
            Log.warn("Project " + projectName + " failed to be created. Caused by: " + e.getMessage() + ". Trying again.");
        }

        return created;
    }

    private List<HasMetadata> loadYaml(String template) {
        return client.load(new ByteArrayInputStream(template.getBytes())).get();
    }

    private String generateRandomProjectName() {
        return ThreadLocalRandom.current().ints(PROJECT_NAME_SIZE, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
