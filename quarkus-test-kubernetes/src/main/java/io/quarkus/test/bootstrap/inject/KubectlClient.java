package io.quarkus.test.bootstrap.inject;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;

public final class KubectlClient {

    private static final PropertyLookup ENABLED_EPHEMERAL_NAMESPACES = new PropertyLookup(
            "ts.kubernetes.ephemeral.namespaces.enabled", Boolean.TRUE.toString());

    private static final String RESOURCE_PREFIX = "resource::";
    private static final String RESOURCE_MNT_FOLDER = "/resource";
    private static final int NAMESPACE_NAME_SIZE = 10;
    private static final int NAMESPACE_CREATION_RETRIES = 5;
    private static final String LABEL_TO_WATCH_FOR_LOGS = "tsLogWatch";
    private static final String LABEL_SCENARIO_ID = "scenarioId";

    private static final String KUBECTL = "kubectl";
    private static final int HTTP_PORT_DEFAULT = 80;

    private final String currentNamespace;
    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;
    private final String scenarioId;

    private KubectlClient(String scenarioUniqueName) {
        this.scenarioId = scenarioUniqueName;
        String activeNamespace = new DefaultOpenShiftClient().getNamespace();
        currentNamespace = ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean() ? createNamespace() : activeNamespace;
        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(currentNamespace).build();
        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(currentNamespace);
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
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " for " + service.getName() + ". Caused by "
                    + e.getMessage());
        }
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
                    "--type=LoadBalancer", "-n", currentNamespace).runAndWait();
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
     * Resolve the url by the service.
     *
     * @param service
     * @return
     */
    public String url(Service service) {
        String serviceName = service.getName();
        io.fabric8.kubernetes.api.model.Service serviceModel = client.services().withName(serviceName).get();
        if (serviceModel == null
                || serviceModel.getStatus() == null
                || serviceModel.getStatus().getLoadBalancer() == null
                || serviceModel.getStatus().getLoadBalancer().getIngress() == null) {
            printServiceInfo(service);
            fail("Service " + serviceName + " not found");
        }

        // IP detection rules:
        // 1.- Try Ingress IP
        // 2.- Try Ingress Hostname
        Optional<String> ip = serviceModel.getStatus().getLoadBalancer().getIngress().stream()
                .map(ingress -> StringUtils.defaultIfBlank(ingress.getIp(), ingress.getHostname()))
                .filter(StringUtils::isNotEmpty)
                .findFirst();

        if (ip.isEmpty()) {
            printServiceInfo(service);
            fail("Service " + serviceName + " host not found");
        }

        return "http://" + ip.get();
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
                .map(ServicePort::getPort)
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
                masterClient.close();
            }
        } else {
            deleteResourcesByLabel(LABEL_SCENARIO_ID, getScenarioId());
        }
    }

    private String getScenarioId() {
        return scenarioId;
    }

    /**
     * Delete test resources.
     */
    private void deleteResourcesByLabel(String labelName, String labelValue) {
        try {
            String label = String.format("%s=%s", labelName, labelValue);
            new Command(KUBECTL, "delete", "-n", currentNamespace, "all", "-l", label).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
    }

    private String extractNamespace(String namespace) {
        return namespace.split(":")[1];
    }

    private boolean isPodRunning(Pod pod) {
        return pod.getStatus().getPhase().equals("Running");
    }

    private List<HasMetadata> loadYaml(String template) {
        return client.load(new ByteArrayInputStream(template.getBytes())).get();
    }

    private String enrichTemplate(Service service, String template, Map<String, String> extraTemplateProperties) {
        List<HasMetadata> objs = loadYaml(template);
        for (HasMetadata obj : objs) {
            // set namespace
            obj.getMetadata().setNamespace(namespace());
            Map<String, String> objMetadataLabels = Optional.ofNullable(obj.getMetadata().getLabels())
                    .orElse(new HashMap<>());

            objMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());
            obj.getMetadata().setLabels(objMetadataLabels);

            if (obj instanceof Deployment) {
                Deployment d = (Deployment) obj;

                // set deployment name
                d.getMetadata().setName(service.getName());

                // set metadata to template
                d.getSpec().getTemplate().getMetadata().setNamespace(namespace());

                // add label for logs
                Map<String, String> templateMetadataLabels = d.getSpec().getTemplate().getMetadata().getLabels();
                templateMetadataLabels.put(LABEL_TO_WATCH_FOR_LOGS, service.getName());
                templateMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());

                // add env var properties
                Map<String, String> enrichProperties = enrichProperties(service.getProperties(), d);
                enrichProperties.putAll(extraTemplateProperties);
                d.getSpec().getTemplate().getSpec().getContainers()
                        .forEach(container -> enrichProperties.entrySet().forEach(property -> {
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
            fail("Failed adding properties into template. Caused by " + e.getMessage());
        }

        return template;
    }

    private EnvVar getEnvVarByKey(String key, Container container) {
        return container.getEnv().stream().filter(env -> StringUtils.equals(key, env.getName())).findFirst().orElse(null);
    }

    private Map<String, String> enrichProperties(Map<String, String> properties, Deployment deployment) {
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
                deployment.getSpec().getTemplate().getSpec().getVolumes().add(new VolumeBuilder().withName(configMapName)
                        .withConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build()).build());

                // Configure all the containers to map the volume
                deployment.getSpec().getTemplate().getSpec().getContainers()
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

    private void printServiceInfo(Service service) {
        try {
            new Command(KUBECTL, "get", "svc", service.getName(), "-n", currentNamespace)
                    .outputToConsole()
                    .runAndWait();
        } catch (Exception ignored) {
        }
    }

}
