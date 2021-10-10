package io.quarkus.test.bootstrap.inject;

import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsNotNull;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SLASH;
import static io.quarkus.test.utils.PropertiesUtils.TARGET;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupSpec;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionSpec;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.operator.model.CustomResourceStatus;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;

public final class OpenShiftClient {

    public static final String LABEL_TO_WATCH_FOR_LOGS = "tsLogWatch";
    public static final String LABEL_SCENARIO_ID = "scenarioId";

    private static final String IMAGE_STREAM_TIMEOUT = "imagestream.install.timeout";
    private static final String OPERATOR_INSTALL_TIMEOUT = "operator.install.timeout";
    private static final Duration TIMEOUT_DEFAULT = Duration.ofMinutes(5);
    private static final int PROJECT_NAME_SIZE = 10;
    private static final int PROJECT_CREATION_RETRIES = 5;
    private static final String OPERATOR_PHASE_INSTALLED = "Succeeded";
    private static final String BUILD_FAILED_STATUS = "Failed";
    private static final String CUSTOM_RESOURCE_EXPECTED_TYPE = "Ready";
    private static final String CUSTOM_RESOURCE_EXPECTED_STATUS = "True";
    private static final String RESOURCE_MNT_FOLDER = "/resources";

    private static final String OC = "oc";

    private static final PropertyLookup ENABLED_EPHEMERAL_NAMESPACES = new PropertyLookup(
            "ts.openshift.ephemeral.namespaces.enabled", Boolean.TRUE.toString());

    private final String currentNamespace;
    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;
    private final KnativeClient kn;
    private final String scenarioId;

    private OpenShiftClient(String scenarioId) {
        this.scenarioId = scenarioId;
        String activeNamespace = new DefaultOpenShiftClient().getNamespace();
        currentNamespace = ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean() ? createProject() : activeNamespace;
        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(currentNamespace).build();
        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(currentNamespace);
        kn = client.adapt(KnativeClient.class);
    }

    public static OpenShiftClient create(String scenarioId) {
        return new OpenShiftClient(scenarioId);
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
    public void apply(Path file) {
        applyInProject(file, currentNamespace);
    }

    /**
     * Apply the file into OpenShift.
     *
     * @param file YAML file to apply
     * @param project where to apply the YAML file.
     */
    public void applyInProject(Path file, String project) {
        try {
            new Command(OC, "apply", "-f", file.toAbsolutePath().toString(), "-n", project).runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " . Caused by " + e.getMessage());
        }
    }

    /**
     * Delete the file into OpenShift.
     *
     * @param file
     */
    public void delete(Path file) {
        deleteInProject(file, currentNamespace);
    }

    /**
     * Delete the file into OpenShift.
     *
     * @param file YAML file to apply
     * @param project where to apply the YAML file.
     */
    public void deleteInProject(Path file, String project) {
        try {
            new Command(OC, "delete", "-f", file.toAbsolutePath().toString(), "-n", project).runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " . Caused by " + e.getMessage());
        }
    }

    /**
     * Update the file and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     */
    public void applyServicePropertiesUsingTemplate(Service service, String file, UnaryOperator<String> update, Path target) {
        applyServicePropertiesUsingTemplate(service, file, update, Collections.emptyMap(), target);
    }

    /**
     * Update the file with extra template properties, and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     */
    public void applyServicePropertiesUsingTemplate(Service service, String file, UnaryOperator<String> update,
            Map<String, String> extraTemplateProperties, Path target) {
        String content = FileUtils.loadFile(file);
        content = enrichTemplate(service, update.apply(content), extraTemplateProperties);
        apply(FileUtils.copyContentTo(content, target));
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
        expose(service.getName(), port);
    }

    /**
     * Expose the service and port defined by service name.
     *
     * @param serviceName
     * @param port
     */
    public void expose(String serviceName, int port) {
        Route route = client.routes().withName(serviceName).get();
        if (route != null) {
            // already exposed.
            return;
        }

        try {
            new Command(OC, "expose", "svc/" + serviceName, "--port=" + port, "-n", currentNamespace,
                    "-l" + LABEL_SCENARIO_ID + "=" + getScenarioId()).runAndWait();
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
        if (isServerlessService(service.getName())) {
            return;
        }

        try {
            new Command(OC, "scale", "dc/" + service.getName(), "--replicas=" + replicas, "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be scaled. Caused by " + e.getMessage());
        }
    }

    /**
     * Waits until the Build Config finishes.
     *
     * @param buildConfigName
     */
    public void followBuildConfigLogs(String buildConfigName) {
        untilIsNotNull(client.buildConfigs().withName(buildConfigName)::get);
        try {
            new Command(OC, "logs", "bc/" + buildConfigName, "--follow", "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Log retrieval from bc failed. Caused by " + e.getMessage());
        }

        if (isBuildFailed(buildConfigName)) {
            fail("Build failed");
        }
    }

    /**
     * Check whether the build failed.
     *
     * @param buildConfigName
     * @return
     */
    public boolean isBuildFailed(String buildConfigName) {
        return client.builds().withLabel("buildconfig", buildConfigName).list().getItems().stream()
                .anyMatch(build -> BUILD_FAILED_STATUS.equals(build.getStatus().getPhase()));
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
     * Get the running pods in the current service.
     */
    public List<Pod> podsInService(Service service) {
        return client.pods().withLabel(LABEL_TO_WATCH_FOR_LOGS, service.getName()).list().getItems();
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
            PodResource<Pod> resource = client.pods().withName(podName);
            for (Container container : pod.getSpec().getContainers()) {
                logs.put(podName + "-" + container.getName(), resource.inContainer(container.getName()).getLog());
            }
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
                PodResource<Pod> resource = client.pods().withName(podName);
                for (Container container : pod.getSpec().getContainers()) {
                    logs.put(podName + "-" + container.getName(), resource.inContainer(container.getName()).getLog());
                }
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
        return url(service.getName());
    }

    /**
     * Resolve the url by the service name.
     *
     * @param serviceName
     * @return
     */
    public String url(String serviceName) {
        if (isServerlessService(serviceName)) {
            io.fabric8.knative.serving.v1.Route knRoute = kn.routes().withName(serviceName).get();
            return knRoute.getStatus().getUrl();
        }

        Route route = client.routes().withName(serviceName).get();
        if (route == null || route.getSpec() == null) {
            fail("Route for service " + serviceName + " not found");
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
                if ((obj instanceof ImageStream)
                        && !StringUtils.equals(obj.getMetadata().getName(), service.getName())) {
                    ImageStream is = (ImageStream) obj;
                    untilIsTrue(() -> hasImageStreamTags(is),
                            AwaitilitySettings.defaults().withService(service)
                                    .usingTimeout(
                                            service.getConfiguration().getAsDuration(IMAGE_STREAM_TIMEOUT, TIMEOUT_DEFAULT)));
                }
            }
        } catch (IOException e) {
            fail("Fail to load the file " + file + ". Caused by " + e.getMessage());
        }
    }

    public void installOperator(Service service, String name, String channel, String source, String sourceNamespace) {
        if (!ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            throw new UnsupportedOperationException("Operators not supported with ephemeral namespaces disabled");
        }

        // Install the operator group
        OperatorGroup groupModel = new OperatorGroup();
        groupModel.setMetadata(new ObjectMeta());
        groupModel.getMetadata().setName(service.getName());
        groupModel.setSpec(new OperatorGroupSpec());
        groupModel.getSpec().setTargetNamespaces(Arrays.asList(currentNamespace));
        client.resource(groupModel).createOrReplace();

        // Install the subscription
        Subscription subscriptionModel = new Subscription();
        subscriptionModel.setMetadata(new ObjectMeta());
        subscriptionModel.getMetadata().setName(name);
        subscriptionModel.getMetadata().setNamespace(currentNamespace);

        subscriptionModel.setSpec(new SubscriptionSpec());
        subscriptionModel.getSpec().setChannel(channel);
        subscriptionModel.getSpec().setName(name);
        subscriptionModel.getSpec().setSource(source);
        subscriptionModel.getSpec().setSourceNamespace(sourceNamespace);

        Log.info("Installing operator... %s", service.getName());
        client.operatorHub().subscriptions().create(subscriptionModel);

        // Wait for the operator to be installed
        untilIsTrue(() -> {
            // Get Cluster Service Version
            Subscription subscription = client.operatorHub().subscriptions().withName(name).get();
            String installedCsv = subscription.getStatus().getInstalledCSV();
            if (StringUtils.isEmpty(installedCsv)) {
                return false;
            }

            // Check Cluster Service Version status
            ClusterServiceVersion operatorService = client.operatorHub().clusterServiceVersions().withName(installedCsv)
                    .get();
            return OPERATOR_PHASE_INSTALLED.equals(operatorService.getStatus().getPhase());
        }, AwaitilitySettings
                .defaults()
                .withService(service)
                .usingTimeout(service.getConfiguration().getAsDuration(OPERATOR_INSTALL_TIMEOUT, TIMEOUT_DEFAULT)));
        Log.info("Operator installed... %s", service.getName());
    }

    /**
     * Check whether the the custom resource to have a condition status "Ready" with value "True".
     */
    public boolean isCustomResourceReady(String name,
            Class<? extends CustomResource<?, ? extends CustomResourceStatus>> crdType) {
        CustomResource<?, ? extends CustomResourceStatus> customResource = client.customResources(crdType).withName(name).get();
        if (customResource == null
                || customResource.getStatus() == null
                || customResource.getStatus().getConditions() == null) {
            return false;
        }

        return customResource.getStatus().getConditions().stream()
                .anyMatch(condition -> CUSTOM_RESOURCE_EXPECTED_TYPE.equals(condition.getType())
                        && CUSTOM_RESOURCE_EXPECTED_STATUS.equals(condition.getStatus()));
    }

    public String execOnPod(String namespace, String podName, String containerId, String... input)
            throws InterruptedException, IOException {

        List<String> output = new ArrayList<>();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(OC, "exec", podName, "-c", containerId, "-n", namespace));
        args.addAll(Arrays.asList(input));
        new Command(args).outputToLines(output).runAndWait();

        return output.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Returns whether the service name is a serverless (knative) service.
     * If the underlying API returns an exception, it will return false.
     *
     * @param serviceName
     * @return
     */
    public boolean isServerlessService(String serviceName) {
        try {
            return kn.services().withName(serviceName).get() != null;
        } catch (Exception ex) {
            Log.warn("Failed to check serverless service. Will assume it's not serverless", ex);
            return false;
        }
    }

    /**
     * @return status of the namespace.
     */
    public String getEvents() {
        List<String> output = new ArrayList<>();
        try {
            new Command(OC, "get", "events", "-n", currentNamespace).outputToLines(output).runAndWait();
        } catch (Exception ex) {
            Log.warn("Failed to get project status", ex);
        }

        return output.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * @return status of the namespace.
     */
    public String getStatus() {
        List<String> output = new ArrayList<>();
        try {
            new Command(OC, "status", "--suggest", "-n", currentNamespace).outputToLines(output).runAndWait();
        } catch (Exception ex) {
            Log.warn("Failed to get project status", ex);
        }

        return output.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Delete the current project and all its resources.
     */
    public void deleteProject() {
        if (ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            try {
                new Command(OC, "delete", "project", currentNamespace).runAndWait();
            } catch (Exception e) {
                fail("Project failed to be deleted. Caused by " + e.getMessage());
            } finally {
                masterClient.close();
            }
        } else {
            deleteResourcesByLabel(LABEL_SCENARIO_ID, getScenarioId());
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }

    /**
     * Delete test resources.
     */
    private void deleteResourcesByLabel(String labelName, String labelValue) {
        try {
            String label = String.format("%s=%s", labelName, labelValue);
            new Command(OC, "delete", "-n", currentNamespace, "all", "-l", label).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
    }

    private String enrichTemplate(Service service, String template, Map<String, String> extraTemplateProperties) {
        List<HasMetadata> objs = loadYaml(template);
        for (HasMetadata obj : objs) {
            // set namespace
            obj.getMetadata().setNamespace(project());
            Map<String, String> objMetadataLabels = Optional.ofNullable(obj.getMetadata().getLabels())
                    .orElse(new HashMap<>());

            objMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());
            obj.getMetadata().setLabels(objMetadataLabels);

            if (obj instanceof DeploymentConfig) {
                DeploymentConfig dc = (DeploymentConfig) obj;

                // set deployment name
                dc.getMetadata().setName(service.getName());

                // set metadata to template
                dc.getSpec().getTemplate().getMetadata().setNamespace(project());

                // add label for logs and and unique scenarioId
                Map<String, String> templateMetadataLabels = dc.getSpec().getTemplate().getMetadata().getLabels();
                templateMetadataLabels.put(LABEL_TO_WATCH_FOR_LOGS, service.getName());
                templateMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());

                // add env var properties
                Map<String, String> enrichProperties = enrichProperties(service.getProperties(), dc);
                enrichProperties.putAll(extraTemplateProperties);
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
        // mount path x volume
        Map<String, Volume> volumes = new HashMap<>();

        Map<String, String> output = new HashMap<>();
        for (Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();
            if (isResource(entry.getValue())) {
                String path = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String configMapName = normalizeName(mountPath);

                // Update config map
                createOrUpdateConfigMap(configMapName, filename, getFileContent(path));

                // Add the volume
                if (!volumes.containsKey(mountPath)) {
                    Volume volume = new VolumeBuilder()
                            .withName(configMapName)
                            .withConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build())
                            .build();
                    volumes.put(mountPath, volume);
                }

                value = mountPath + SLASH + filename;
            } else if (isSecret(entry.getValue())) {
                String path = entry.getValue().replace(SECRET_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String secretName = normalizeName(path);

                // Push secret file
                doCreateSecretFromFile(secretName, getFilePath(path));

                // Add the volume
                Volume volume = new VolumeBuilder()
                        .withName(secretName)
                        .withSecret(new SecretVolumeSourceBuilder()
                                .withSecretName(secretName)
                                .build())
                        .build();
                volumes.put(mountPath, volume);

                value = mountPath + SLASH + filename;
            }

            output.put(entry.getKey(), value);
        }

        for (Entry<String, Volume> volume : volumes.entrySet()) {
            dc.getSpec().getTemplate().getSpec().getVolumes().add(volume.getValue());

            // Configure all the containers to map the volume
            dc.getSpec().getTemplate().getSpec().getContainers()
                    .forEach(container -> container.getVolumeMounts()
                            .add(new VolumeMountBuilder().withName(volume.getValue().getName())
                                    .withReadOnly(true).withMountPath(volume.getKey()).build()));
        }

        return output;
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
            client.configMaps().createOrReplace(new ConfigMapBuilder()
                    .withNewMetadata().withName(configMapName).endMetadata()
                    .addToData(key, value)
                    .build());
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

    private String normalizeName(String name) {
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

    private boolean hasImageStreamTags(ImageStream is) {
        return !masterClient.imageStreams().withName(is.getMetadata().getName()).get().getStatus().getTags().isEmpty();
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

    private void doCreateSecretFromFile(String name, String filePath) {
        if (client.secrets().withName(name).get() == null) {
            try {
                new Command(OC, "create", "secret", "generic", name, "--from-file=" + filePath,
                        "-n", currentNamespace).runAndWait();
            } catch (Exception e) {
                fail("Could not create secret. Caused by " + e.getMessage());
            }
        }
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
