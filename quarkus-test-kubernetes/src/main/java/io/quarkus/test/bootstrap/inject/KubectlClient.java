package io.quarkus.test.bootstrap.inject;

import static io.quarkus.test.model.CustomVolume.VolumeType.CONFIG_MAP;
import static io.quarkus.test.model.CustomVolume.VolumeType.SECRET;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX_MATCHER;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_SPLIT_CHAR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SLASH;
import static io.quarkus.test.utils.PropertiesUtils.TARGET;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.impl.KubernetesClientImpl;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.model.CustomVolume;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;

public final class KubectlClient {

    public static final String LABEL_TO_WATCH_FOR_LOGS = "tsLogWatch";
    public static final String LABEL_SCENARIO_ID = "scenarioId";
    public static final PropertyLookup ENABLED_EPHEMERAL_NAMESPACES = new PropertyLookup(
            "ts.kubernetes.ephemeral.namespaces.enabled", Boolean.TRUE.toString());
    private static final String RESOURCE_MNT_FOLDER = "/resource";
    private static final int NAMESPACE_NAME_SIZE = 10;
    private static final int NAMESPACE_CREATION_RETRIES = 5;

    private static final int DEPLOYMENT_CREATION_TIMEOUT = 30;

    private static final String KUBECTL = "kubectl";
    private static final int HTTP_PORT_DEFAULT = 80;
    private final String currentNamespace;
    private final KubernetesClientImpl client;
    private final String scenarioId;

    private KubectlClient(String scenarioUniqueName) {
        this.scenarioId = scenarioUniqueName;
        if (ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            currentNamespace = createNamespace();
            Config config = new ConfigBuilder().withTrustCerts(true).withNamespace(currentNamespace).build();
            client = createClient(config);
        } else {
            Config config = new ConfigBuilder().withTrustCerts(true).build();
            client = createClient(config);
            currentNamespace = client.getNamespace();
        }
        setCurrentSessionNamespace(currentNamespace);
    }

    private static KubernetesClientImpl createClient(Config config) {
        return new KubernetesClientBuilder().withConfig(config).build()
                .adapt(KubernetesClientImpl.class);
    }

    public static KubectlClient create(String scenarioName) {
        return new KubectlClient(scenarioName);
    }

    /**
     * @return the current namespace
     */
    public String namespace() {
        return currentNamespace;
    }

    /**
     * Apply the file into Kubernetes.
     *
     * @param file
     */
    public void apply(Service service, Path file) {
        try {
            new Command(KUBECTL, "apply", "-f", file.toAbsolutePath().toString(), "-n", currentNamespace)
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath() + " for " + service.getName() + ". Caused by "
                    + e.getMessage());
        }
    }

    /**
     * Update the deployment config using the service properties.
     *
     * @param service
     */
    public void applyServicePropertiesUsingDeploymentConfig(Service service) {
        Deployment deployment = client.apps().deployments().withName(service.getName()).get();
        Map<String, String> enrichProperties = enrichProperties(service.getProperties(), deployment);

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
            enrichProperties.forEach((key, value) -> container.getEnv().add(new EnvVar(key, value, null)));
        });

        client.apps().deployments().withTimeout(DEPLOYMENT_CREATION_TIMEOUT, TimeUnit.SECONDS).delete();
        client.apps().deployments().resource(deployment).create();
    }

    /**
     * Update the file and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     */
    public void applyServiceProperties(Service service, String file, UnaryOperator<String> update, Path target) {
        applyServiceProperties(service, file, update, Collections.emptyMap(), target);
    }

    /**
     * Update the file with extra template properties and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     */
    public void applyServiceProperties(Service service, String file, UnaryOperator<String> update,
            Map<String, String> extraTemplateProperties, Path target) {
        String content = FileUtils.loadFile(file);
        content = enrichTemplate(service, update.apply(content), extraTemplateProperties);
        apply(service, FileUtils.copyContentTo(content, target));
    }

    /**
     * Expose the service and port defined.
     *
     * @param service
     * @param port
     */
    public void expose(Service service, Integer port) {
        try {
            new Command(KUBECTL, "expose", "deployment", service.getName(), "--port=" + port, "--name=" + service.getName(),
                    "--type=NodePort", "-n", currentNamespace).runAndWait();
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
            new Command(KUBECTL, "scale", "deployment/" + service.getName(), "--replicas=" + replicas, "-n", currentNamespace)
                    .runAndWait();
        } catch (Exception e) {
            fail("Service failed to be scaled. Caused by " + e.getMessage());
        }
    }

    /**
     * Get the running pods in the current service.
     */
    public List<Pod> podsInService(Service service) {
        return client.pods().withLabel(LABEL_TO_WATCH_FOR_LOGS, service.getName()).list().getItems();
    }

    /**
     * Get all the logs for all the pods within the current namespace.
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
        for (Pod pod : podsInService(service)) {
            if (isPodRunning(pod)) {
                String podName = pod.getMetadata().getName();
                logs.put(podName, client.pods().withName(podName).getLog());
            }
        }

        return logs;
    }

    /**
     * Get node host IP.
     */
    public String host() {
        String nodeURL = client.network().getConfiguration().getMasterUrl();
        try {
            URI uri = new URI(nodeURL);
            return uri.getHost();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Resolve the port by the service.
     *
     * @param service
     * @return
     */
    public int port(Service service) {
        String serviceName = service.getName();
        io.fabric8.kubernetes.api.model.Service serviceModel = client.services().withName(serviceName).get();
        if (serviceModel == null || serviceModel.getSpec() == null || serviceModel.getSpec().getPorts() == null) {
            fail("Service " + serviceName + " not found");
        }

        return serviceModel.getSpec().getPorts().stream()
                .map(ServicePort::getNodePort)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(HTTP_PORT_DEFAULT);
    }

    /**
     * Delete the namespace and all the resources.
     */
    public void deleteNamespace() {
        if (ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            try {
                new Command(KUBECTL, "delete", "namespace", currentNamespace).runAndWait();
            } catch (Exception e) {
                fail("Project failed to be deleted. Caused by " + e.getMessage());
            } finally {
                client.close();
            }
        } else {
            deleteResources(getScenarioId());
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }

    /**
     * Delete test resources.
     */
    private void deleteResources(String labelValue) {
        try {
            String label = String.format("%s=%s", KubectlClient.LABEL_SCENARIO_ID, labelValue);
            new Command(KUBECTL, "delete", "-n", currentNamespace, "all", "-l", label).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            client.close();
        }
    }

    private boolean isPodRunning(Pod pod) {
        return pod.getStatus().getPhase().equals("Running");
    }

    private List<HasMetadata> loadYaml(String template) {
        NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata> load = client
                .load(new ByteArrayInputStream(template.getBytes()));
        return load.items();
    }

    private String enrichTemplate(Service service, String template, Map<String, String> extraTemplateProperties) {
        List<HasMetadata> objects = loadYaml(template);
        for (HasMetadata obj : objects) {
            obj.getMetadata().setNamespace(namespace());
            Map<String, String> objMetadataLabels = Optional.ofNullable(obj.getMetadata().getLabels())
                    .orElse(new HashMap<>());

            objMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());
            obj.getMetadata().setLabels(objMetadataLabels);

            if (obj instanceof Deployment deployment) {

                // set deployment name
                deployment.getMetadata().setName(service.getName());

                // set metadata to template
                deployment.getSpec().getTemplate().getMetadata().setNamespace(namespace());

                // add label for logs
                Map<String, String> templateMetadataLabels = deployment.getSpec().getTemplate().getMetadata().getLabels();
                templateMetadataLabels.put(LABEL_TO_WATCH_FOR_LOGS, service.getName());
                templateMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());

                // add env var properties
                Map<String, String> enrichProperties = enrichProperties(service.getProperties(), deployment);
                enrichProperties.putAll(extraTemplateProperties);
                deployment.getSpec().getTemplate().getSpec().getContainers()
                        .forEach(container -> enrichProperties.forEach((key, value) -> {
                            EnvVar envVar = getEnvVarByKey(key, container);
                            if (envVar == null) {
                                container.getEnv().add(new EnvVar(key, value, null));
                            } else {
                                envVar.setValue(value);
                            }
                        }));
            }
        }

        KubernetesList list = new KubernetesList();
        list.setItems(objects);
        return Serialization.asYaml(list);
    }

    private EnvVar getEnvVarByKey(String key, Container container) {
        return container.getEnv().stream().filter(env -> StringUtils.equals(key, env.getName())).findFirst().orElse(null);
    }

    private Map<String, String> enrichProperties(Map<String, String> properties, Deployment deployment) {
        // mount path x volume
        Map<String, CustomVolume> volumes = new HashMap<>();

        Map<String, String> output = new HashMap<>();
        for (Entry<String, String> entry : properties.entrySet()) {
            String propertyValue = entry.getValue();
            if (isResource(entry.getValue())) {
                String path = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String configMapName = normalizeConfigMapName(mountPath);

                // Update config map
                createOrUpdateConfigMap(configMapName, filename, getFileContent(path));

                // Add the volume
                if (!volumes.containsKey(mountPath)) {
                    volumes.put(mountPath, new CustomVolume(configMapName, "", CONFIG_MAP));
                }

                propertyValue = mountPath + SLASH + filename;
            } else if (isResourceWithDestinationPath(propertyValue)) {
                String path = propertyValue.replace(RESOURCE_WITH_DESTINATION_PREFIX, StringUtils.EMPTY);
                if (!propertyValue.matches(RESOURCE_WITH_DESTINATION_PREFIX_MATCHER)) {
                    String errorMsg = String.format("Unexpected %s format. Expected destinationPath|fileName but found %s",
                            RESOURCE_WITH_DESTINATION_PREFIX, propertyValue);
                    throw new RuntimeException(errorMsg);
                }

                String mountPath = path.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[0];
                String fileName = path.split(RESOURCE_WITH_DESTINATION_SPLIT_CHAR)[1];
                String fileNameNormalized = getFileName(fileName);
                String configMapName = normalizeConfigMapName(mountPath);

                // Update config map
                createOrUpdateConfigMap(configMapName, fileNameNormalized, getFileContent(fileName));
                propertyValue = mountPath + SLASH + fileNameNormalized;
                // Add the volume
                if (!volumes.containsKey(mountPath)) {
                    volumes.put(propertyValue, new CustomVolume(configMapName, fileNameNormalized, CONFIG_MAP));
                }
            } else if (isSecret(entry.getValue())) {
                String path = entry.getValue().replace(SECRET_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String secretName = normalizeConfigMapName(path);

                // Push secret file
                doCreateSecretFromFile(secretName, getFilePath(path));
                volumes.put(mountPath, new CustomVolume(secretName, "", SECRET));
                propertyValue = mountPath + SLASH + filename;
            }

            output.put(entry.getKey(), propertyValue);
        }

        for (Entry<String, CustomVolume> volume : volumes.entrySet()) {
            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume.getValue().getVolume());

            // Configure all the containers to map the volume
            deployment.getSpec().getTemplate().getSpec().getContainers()
                    .forEach(container -> container.getVolumeMounts()
                            .add(createVolumeMount(volume)));
        }

        return output;
    }

    private VolumeMount createVolumeMount(Entry<String, CustomVolume> volume) {
        VolumeMountBuilder volumeMountBuilder = new VolumeMountBuilder().withName(volume.getValue().getName())
                .withReadOnly(true).withMountPath(volume.getKey());

        if (!volume.getValue().getSubFolderRegExp().isEmpty()) {
            volumeMountBuilder.withSubPathExpr(volume.getValue().getSubFolderRegExp());
        }

        return volumeMountBuilder.build();
    }

    private boolean isResourceWithDestinationPath(String key) {
        return key.startsWith(RESOURCE_WITH_DESTINATION_PREFIX);
    }

    private void createOrUpdateConfigMap(String configMapName, String key, String value) {
        if (client.configMaps().withName(configMapName).get() != null) {
            // update existing config map by adding new file
            client.configMaps().withName(configMapName)
                    .edit(configMap -> {
                        configMap.getData().put(key, value);
                        return configMap;
                    });
        } else {
            // create new one
            client.configMaps().resource(new ConfigMapBuilder()
                    .withNewMetadata().withName(configMapName).endMetadata()
                    .addToData(key, value)
                    .build()).createOr(NonDeletingOperation::update);
        }
    }

    private void doCreateSecretFromFile(String name, String filePath) {
        if (client.secrets().withName(name).get() == null) {
            try {
                new Command(KUBECTL, "create", "secret", "generic", name, "--from-file=" + filePath,
                        "-n", currentNamespace).runAndWait();
            } catch (Exception e) {
                fail("Could not create secret. Caused by " + e.getMessage());
            }
        }
    }

    private String getFileName(String path) {
        if (!path.contains(SLASH)) {
            return path;
        }

        return path.substring(path.lastIndexOf(SLASH) + 1);
    }

    private String getMountPath(String path) {
        if (!path.contains(SLASH)) {
            return RESOURCE_MNT_FOLDER;
        }

        String mountPath = StringUtils.defaultIfEmpty(path.substring(0, path.lastIndexOf(SLASH)), RESOURCE_MNT_FOLDER);
        if (!path.startsWith(SLASH)) {
            mountPath = SLASH + mountPath;
        }

        return mountPath;
    }

    private String getFileContent(String path) {
        String filePath = getFilePath(path);
        if (Files.exists(Path.of(filePath))) {
            // from file system
            return FileUtils.loadFile(Path.of(filePath).toFile());
        }

        // from classpath
        return FileUtils.loadFile(filePath);
    }

    private String getFilePath(String path) {
        try (Stream<Path> binariesFound = Files
                .find(TARGET, Integer.MAX_VALUE,
                        (file, basicFileAttributes) -> file.toString().contains(path))) {
            return binariesFound.map(Path::toString).findFirst().orElse(path);
        } catch (IOException ex) {
            // ignored
        }

        return path;
    }

    private String normalizeConfigMapName(String name) {
        return StringUtils.removeStart(name, SLASH)
                .replaceAll(Pattern.quote("."), "-")
                .replaceAll(SLASH, "-");
    }

    private boolean isResource(String key) {
        return key.startsWith(RESOURCE_PREFIX);
    }

    private boolean isSecret(String key) {
        return key.startsWith(SECRET_PREFIX);
    }

    private String createNamespace() {
        boolean namespaceCreated = false;

        String namespace = generateRandomNamespaceName();
        int index = 0;
        while (index < NAMESPACE_CREATION_RETRIES) {
            if (doCreateNamespace(namespace)) {
                namespaceCreated = true;
                break;
            }

            namespace = generateRandomNamespaceName();
            index++;
        }

        if (!namespaceCreated) {
            fail("Namespace cannot be created. Review your Kubernetes installation.");
        }

        return namespace;
    }

    private boolean doCreateNamespace(String namespaceName) {
        boolean created = false;
        try {
            new Command(KUBECTL, "create", "namespace", namespaceName).runAndWait();
            created = true;
        } catch (Exception e) {
            Log.warn("Namespace " + namespaceName + " failed to be created. Caused by: " + e.getMessage() + ". Trying again.");
        }

        return created;
    }

    private String generateRandomNamespaceName() {
        return ThreadLocalRandom.current().ints(NAMESPACE_NAME_SIZE, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void setCurrentSessionNamespace(String namespaceName) {
        try {
            new Command(KUBECTL, "config", "set-context", "--current", "--namespace=" + namespaceName).runAndWait();
        } catch (Exception e) {
            Log.warn("Namespace " + namespaceName
                    + " failed to be set as current session namespace. Caused by: " + e.getMessage() + ". Trying again.");
        }
    }
}
