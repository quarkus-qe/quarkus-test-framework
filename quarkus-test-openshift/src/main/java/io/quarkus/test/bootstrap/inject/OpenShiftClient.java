package io.quarkus.test.bootstrap.inject;

import static io.quarkus.test.model.CustomVolume.VolumeType.CONFIG_MAP;
import static io.quarkus.test.model.CustomVolume.VolumeType.SECRET;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getAnnotatedConfigMap;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getMountConfigMap;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getMountSecret;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.isAnnotatedConfigMap;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.isMountConfigMap;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.isMountSecret;
import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsNotNull;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX_MATCHER;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_SPLIT_CHAR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.opentest4j.AssertionFailedError;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupSpec;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionSpec;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.fabric8.openshift.client.impl.OpenShiftClientImpl;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.model.CustomVolume;
import io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.operator.model.CustomResourceStatus;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.AwaitilityUtils;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.smallrye.config.common.utils.StringUtil;

public final class OpenShiftClient {

    public static final String LABEL_TO_WATCH_FOR_LOGS = "tsLogWatch";
    public static final String LABEL_SCENARIO_ID = "scenarioId";
    public static final PropertyLookup ENABLED_EPHEMERAL_NAMESPACES = new PropertyLookup(
            "ts.openshift.ephemeral.namespaces.enabled", Boolean.TRUE.toString());

    private static final Logger LOG = Logger.getLogger(OpenShiftClient.class);
    private static final String IMAGE_STREAM_TIMEOUT = "imagestream.install.timeout";
    private static final String OPERATOR_INSTALL_TIMEOUT = "operator.install.timeout";
    private static final Duration TIMEOUT_DEFAULT = Duration.ofMinutes(5);
    private static final int PROJECT_NAME_SIZE = 10;
    private static final int PROJECT_CREATION_RETRIES = 5;
    private static final int SPECS_SECRET_NAME_LIMIT = 63;
    private static final String OPERATOR_PHASE_INSTALLED = "Succeeded";
    private static final String BUILD_FAILED_STATUS = "Failed";
    private static final String CUSTOM_RESOURCE_EXPECTED_TYPE = "Ready";
    private static final String CUSTOM_RESOURCE_EXPECTED_STATUS = "True";
    private static final String RESOURCE_MNT_FOLDER = "/resources";
    private static final String APP_PROPS_CONFIG_MAP_KEY = "application-properties";

    private static final String OC = "oc";

    private final String currentNamespace;
    private final OpenShiftClientImpl client;
    private final KnativeClient kn;
    private final String scenarioId;
    private boolean isClientReady;

    private OpenShiftClient(String scenarioId) {
        this.scenarioId = scenarioId;
        if (ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            currentNamespace = createProject();
            OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(currentNamespace).build();
            client = createClient(config);
        } else {
            OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).build();
            client = createClient(config);
            currentNamespace = client.getNamespace();
        }
        isClientReady = true;
        kn = client.adapt(KnativeClient.class);
    }

    private static OpenShiftClientImpl createClient(OpenShiftConfig config) {
        return new KubernetesClientBuilder().withConfig(config).build()
                .adapt(NamespacedOpenShiftClient.class)
                .adapt(OpenShiftClientImpl.class);
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
        Log.info("Enriching template %s to create a deployment file %s ", file, target);
        String content = FileUtils.loadFile(file);
        content = enrichTemplate(service, update.apply(content), extraTemplateProperties);
        apply(FileUtils.copyContentTo(content, target));
    }

    /**
     * Update the deployment and service using the service properties.
     *
     * @param service
     */
    public void applyServicePropertiesToDeployment(Service service) {
        var serviceName = service.getName();
        Deployment deployment = client.apps().deployments().withName(serviceName).get();
        boolean isQuarkusRuntime = isQuarkusRuntime(deployment.getSpec().getTemplate().getMetadata().getLabels());
        Map<String, String> enrichProperties = enrichProperties(service.getProperties(), deployment, isQuarkusRuntime);
        if (isQuarkusRuntime) {
            updateAnnotationsIfNecessary(service, serviceName);
        }

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
            enrichProperties.forEach((key, value) -> container.getEnv().add(new EnvVar(key, value, null)));
        });
        client.apps().deployments().resource(deployment).unlock().createOr(NonDeletingOperation::patch);
    }

    private void updateAnnotationsIfNecessary(Service service, String serviceName) {
        var annotations = collectAnnotations(service);
        if (!annotations.isEmpty()) {
            var k8ServiceResource = client.services().withName(serviceName);
            AwaitilityUtils.untilIsNotNull(k8ServiceResource::get);
            var k8Service = k8ServiceResource.get();
            boolean edited = false;
            for (var keyToVal : annotations) {
                var annotationName = keyToVal.key();
                if (!k8Service.getMetadata().getAnnotations().containsKey(annotationName)) {
                    k8Service = k8Service.edit().editMetadata().addToAnnotations(annotationName, keyToVal.value()).endMetadata()
                            .build();
                    edited = true;
                }
            }
            if (edited) {
                k8ServiceResource.patch(k8Service);
            }
        }
    }

    /**
     * Start rollout of the service.
     *
     * @param service
     */
    @Deprecated(forRemoval = true)
    //This method is not used anymore. Remove this annotation if you need it for some reason
    public void rollout(Service service) {
        Log.info("Rolling out deployment " + service.getName() + " in namespace " + currentNamespace);
        Log.info("Run this command to replicate: oc rollout latest dc/" + service.getName() + " -n " + currentNamespace);
        Log.info("deployment/" + service.getName() + " rolled out.");
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
     * Expose the service and port on a named route.
     *
     * @param serviceName name of the service to expose
     * @param routeName name of route to access the service
     * @param port port on the service (there can be several of them)
     */
    public void expose(String serviceName, String routeName, int port) {
        Route route = client.routes().withName(routeName).get();
        if (route != null) {
            // already exposed.
            return;
        }

        try {
            new Command(OC, "expose", "svc/" + serviceName,
                    "--port=" + port,
                    "--name=" + routeName,
                    "-n", currentNamespace,
                    "-l" + LABEL_SCENARIO_ID + "=" + getScenarioId()).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    /**
     * Create a service for deployment.
     * Usually done automatically, or inside yamls, so this method should be used only in a very special cases,
     * eg: https://github.com/quarkusio/quarkus/issues/34645
     *
     * @param appName name of the application to expose
     * @param serviceName name of service, which is to be created
     * @param port port on the service and the app (they are the same to keep things simple)
     */
    public void createService(String appName, String serviceName, int port) {
        io.fabric8.kubernetes.api.model.Service route = client.services().withName(serviceName).get();
        if (route != null) {
            // already exposed.
            return;
        }

        try {
            new Command(OC, "expose", "deployment/" + appName,
                    "--port=" + port,
                    "--target-port=" + port,
                    "--name=" + serviceName,
                    "-n", currentNamespace,
                    "-l" + LABEL_SCENARIO_ID + "=" + getScenarioId()).runAndWait();
        } catch (Exception e) {
            fail("Service was not created. Caused by " + e.getMessage());
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
            new Command(OC, "scale",
                    "deployment/" + service.getName(),
                    "--replicas=" + replicas,
                    "-n", currentNamespace)
                    .runAndWait();
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
        Deployment deployment = client.apps().deployments().withName(service.getName()).get();
        return Optional.ofNullable(deployment.getStatus().getReadyReplicas()).orElse(0);
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
            PodResource resource = client.pods().withName(podName);
            for (Container container : pod.getSpec().getContainers()) {
                logs.put(podName + "-" + container.getName(),
                        resource.inContainer(container.getName()).getLog());
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
                PodResource resource = client.pods().withName(podName);
                for (Container container : pod.getSpec().getContainers()) {
                    logs.put(podName + "-" + container.getName(),
                            resource.inContainer(container.getName()).getLog());
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
    public URILike url(Service service) {
        return url(service.getName());
    }

    /**
     * Resolve the url by the service name.
     *
     * @param serviceName
     * @return
     */
    public URILike url(String serviceName) {
        if (isServerlessService(serviceName)) {
            io.fabric8.knative.serving.v1.Route knRoute = kn.routes().withName(serviceName).get();
            return URILike.parse(knRoute.getStatus().getUrl());
        }

        Route route = client.routes().withName(serviceName).get();
        if (route == null || route.getSpec() == null) {
            fail("Route for service " + serviceName + " not found");
        }

        String protocol = route.getSpec().getTls() == null ? "http" : "https";
        String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
        return new URILike(protocol,
                route.getSpec().getHost(),
                -1,
                path);
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
                if ((obj instanceof ImageStream is)
                        && !StringUtils.equals(obj.getMetadata().getName(), service.getName())) {
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
        // call createOr and if it exists the update will be done
        client.resource(groupModel).unlock().createOr(NonDeletingOperation::update);

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
        client.operatorHub().subscriptions().resource(subscriptionModel).create();

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
        if (!isClientReady) {
            return false;
        }
        CustomResource<?, ? extends CustomResourceStatus> customResource = client.resources(crdType).withName(name).get();
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
        List<String> command = Arrays.asList(input);
        if (!command.isEmpty()) {
            args.add("--");
            args.addAll(command);
        }
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
                var projectResource = client.projects().withName(currentNamespace);
                var project = projectResource.get();
                if (project == null) {
                    LOG.infof("Skipping delete operation on project '%s' as project does not exists", currentNamespace);
                    return;
                }

                if (project.isMarkedForDeletion()) {
                    // seems like some other process already tries to delete project
                    LOG.infof("Skipping delete operation on project '%s' as project is already marked "
                            + "for deletion", currentNamespace);
                    return;
                }

                LOG.infof("Deleting project '%s'", currentNamespace);
                if (projectResource.delete().stream().allMatch(Objects::isNull)) {
                    LOG.infof("Project '%s' deleted", currentNamespace);
                    return;
                }

                // something went wrong or the client behavior changed, and it doesn't return null for deleted project
                project = projectResource.update();
                if (project == null) {
                    // project has been deleted after all
                    LOG.warnf("Deleted project '%s', but behavior of 'io.fabric8.kubernetes.client.dsl."
                            + "Deletable.delete' has changed, we need to refactor this operation", currentNamespace);
                    return;
                }

                if (!project.isMarkedForDeletion()) {
                    fail("Illegal state - project '" + currentNamespace + "' should be marked for deletion, "
                            + "please delete project manually");
                }

                var finalizers = project.getFinalizers();
                if (finalizers != null && !finalizers.isEmpty()) {
                    LOG.infof("Attempting to delete finalizers '%s' of project '%s' as the project wasn't cleaned"
                            + " up within default timeout", Arrays.toString(finalizers.toArray()), currentNamespace);
                    finalizers.forEach(project::removeFinalizer);
                    projectResource.patch(project);

                    // let's wait and see
                    AwaitilityUtils.until(projectResource::get, Matchers.nullValue());
                }

                fail("Failed to delete project within default timeout, project is marked for deletion, but we might "
                        + "need too raise waiting period");
            } catch (AssertionFailedError e) {
                fail("Project failed to be deleted. Caused by " + e.getMessage());
            } catch (Exception e) {
                LOG.errorf("Failed to delete project %s", currentNamespace, e);
                fail("Failed to delete OpenShit project");
            } finally {
                client.close();
                isClientReady = false;
            }
        } else {
            deleteResourcesByLabel(LABEL_SCENARIO_ID, getScenarioId());
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public KnativeClient getKnClient() {
        return kn;
    }

    public void removeSecret(String secretName) {
        var secret = client.secrets().withName(secretName);
        if (secret.get() != null) {
            secret.delete();
        }
    }

    public boolean isAnyServicePodReady(String serviceName) {
        return client.pods().withLabel(LABEL_TO_WATCH_FOR_LOGS, serviceName).resources().anyMatch(Resource::isReady);
    }

    private void addAnnotatedConfigMap(String configMapName, String annotationName, String annotationValue) {
        var configMapResource = client.configMaps().withName(configMapName);
        if (configMapResource.get() == null) {
            var configMap = new ConfigMapBuilder()
                    .editMetadata()
                    .withName(configMapName)
                    .addToAnnotations(annotationName, annotationValue)
                    .endMetadata()
                    .build();
            client.resource(configMap).create();
        }
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
            client.close();
            isClientReady = false;
        }
    }

    private String enrichTemplate(Service service, String template, Map<String, String> extraTemplateProperties) {
        var serviceName = service.getName();
        List<HasMetadata> objs = loadYaml(template);
        for (HasMetadata obj : objs) {
            // set namespace
            obj.getMetadata().setNamespace(project());
            Map<String, String> objMetadataLabels = Optional.ofNullable(obj.getMetadata().getLabels())
                    .orElse(new HashMap<>());

            objMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());
            obj.getMetadata().setLabels(objMetadataLabels);

            if (obj instanceof Deployment deployment) {

                // set metadata to template
                deployment.getSpec().getTemplate().getMetadata().setNamespace(project());

                // add label for logs and and unique scenarioId
                Map<String, String> templateMetadataLabels = deployment.getSpec().getTemplate().getMetadata().getLabels();
                templateMetadataLabels.put(LABEL_TO_WATCH_FOR_LOGS, serviceName);
                templateMetadataLabels.put(LABEL_SCENARIO_ID, getScenarioId());
                final boolean isQuarkusRuntime = isQuarkusRuntime(templateMetadataLabels);

                // add env var properties
                Map<String, String> enrichProperties = enrichProperties(service.getProperties(), deployment, isQuarkusRuntime);

                final Map<String, String> environmentVariables;
                if (isQuarkusRuntime) {
                    var propsThatRequireDottedFormat = appPropsThatRequireDottedFormat(enrichProperties);

                    // Configuration properties are only converted to MP Config format for Quarkus runtime for
                    // other runtimes may expect different format.
                    environmentVariables = convertPropertiesToEnvironment(enrichProperties, propsThatRequireDottedFormat);

                    // config properties that contains dashes or other special chars need to be added in dotted
                    // format in a config source with lower priority than env var config source has
                    // see https://quarkus.io/version/main/guides/config-reference#environment-variables for more info
                    var appPropertiesFileContent = createAppPropsForPropsThatRequireDottedFormat(propsThatRequireDottedFormat);
                    if (!appPropertiesFileContent.isEmpty()) {
                        mountPropertiesAsQuarkusAppConfigFile(service, enrichProperties, deployment, appPropertiesFileContent);
                    }
                } else {
                    environmentVariables = enrichProperties;
                }

                environmentVariables.putAll(extraTemplateProperties);
                deployment.getSpec().getTemplate().getSpec().getContainers()
                        .forEach(container -> environmentVariables.forEach((key, value) -> {
                            EnvVar envVar = getEnvVarByKey(key, container);
                            if (envVar == null) {
                                container.getEnv().add(new EnvVar(key, value, null));
                            } else {
                                envVar.setValue(value);
                            }
                        }));
            } else if (obj instanceof io.fabric8.kubernetes.api.model.Service k8Service) {
                boolean isQuarkusRuntime = isQuarkusRuntime(k8Service.getMetadata().getLabels());
                if (isQuarkusRuntime) {
                    collectAnnotations(service).forEach(keyToVal -> {
                        var annotationKey = keyToVal.key();
                        var annotationVal = keyToVal.value();
                        k8Service.getMetadata().getAnnotations().put(annotationKey, annotationVal);
                    });
                }
            }
        }

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(Serialization.asYaml(list).getBytes());
            template = os.toString();
        } catch (IOException e) {
            fail("Failed adding properties into OpenShift template. Caused by " + e.getMessage());
        }

        return template;
    }

    private static List<OpenShiftPropertiesUtils.PropertyToValue> collectAnnotations(Service service) {
        if (isManagementInterfaceService(service)) {
            // Subject Alternative Name (SAN) must match service DNS name,
            // and injected certificates will have SAN set to one of OpenShift services,
            // but we can have 2 services created for one application,
            // one for HTTP server, one for management interface
            // so far, we don't support management interface,
            // which allows mount exactly one secret created for the HTTP server
            return List.of();
        }
        return service.getProperties().values()
                .stream()
                .filter(OpenShiftPropertiesUtils::isAnnotation)
                .map(OpenShiftPropertiesUtils::getServiceAnnotation)
                .toList();
    }

    private static boolean isManagementInterfaceService(Service service) {
        return service.getName().endsWith("-management");
    }

    private String createAppPropsForPropsThatRequireDottedFormat(Map<String, String> configProperties) {
        return configProperties
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue() + System.lineSeparator())
                .collect(Collectors.joining());
    }

    private static boolean isQuarkusRuntime(Map<String, String> templateMetadataLabels) {
        return "quarkus".equals(templateMetadataLabels.get("app.openshift.io/runtime"));
    }

    private static Map<String, String> appPropsThatRequireDottedFormat(Map<String, String> configProperties) {
        return configProperties
                .entrySet()
                .stream()
                .filter(e -> {
                    var configPropertyName = e.getKey();
                    var environmentVariableName = StringUtil.replaceNonAlphanumericByUnderscores(configPropertyName);
                    var configPropertyNameCreatedFromEnvName = StringUtil.toLowerCaseAndDotted(environmentVariableName);
                    return !configPropertyNameCreatedFromEnvName.equalsIgnoreCase(configPropertyName);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void mountPropertiesAsQuarkusAppConfigFile(Service service, Map<String, String> enrichProperties,
            Deployment deployment, String appPropertiesFileContent) {
        var appConfigFileName = service.getName().toLowerCase() + "-quarkus-application-configuration-file";

        // create dedicated config map only with app properties that must exist in dotted config source
        createOrUpdateConfigMap(appConfigFileName, APP_PROPS_CONFIG_MAP_KEY, appPropertiesFileContent);

        // create Quarkus pod volume where we mount config map that contains application.properties
        var configMapVolumeSource = new ConfigMapVolumeSourceBuilder()
                .withName(appConfigFileName)
                .build();
        var volume = new VolumeBuilder()
                .withName(appConfigFileName)
                .withConfigMap(configMapVolumeSource)
                .build();
        deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);

        // $PWD/config/application.properties
        var pwd = QuarkusProperties.isNativeEnabled() ? "/home/quarkus" : "/deployments";

        // mount volume to each deployment config containers
        var volumeMount = new VolumeMountBuilder()
                .withName(appConfigFileName)
                .withSubPath(APP_PROPS_CONFIG_MAP_KEY)
                .withReadOnly(true)
                .withMountPath(pwd + "/config/application.properties")
                .build();
        deployment.getSpec().getTemplate().getSpec().getContainers()
                .forEach(container -> container.getVolumeMounts()
                        .add(volumeMount));
    }

    /**
     * Converts names of configuration properties to the MicroProfile Config specification compliant environment
     * variables format, e.g. quarkus.consul-config.agent.host-port->QUARKUS_CONSUL_CONFIG_AGENT_HOST_PORT
     * <p>
     * see https://quarkus.io/guides/config-reference#environment-variables for details
     */
    private static Map<String, String> convertPropertiesToEnvironment(Map<String, String> properties,
            Map<String, String> propsThatRequireDottedFormat) {
        HashMap<String, String> environment = new HashMap<>(properties.size());
        properties.forEach((property, value) -> {
            // non-Quarkus properties may require different format than MP config format
            // they could also consume environment variables directly but the mapping from env vars is not one to one,
            // therefore we should use Quarkus config source to propagate the information
            if (!propsThatRequireDottedFormat.containsKey(property) || isQuarkusProperty(property)) {
                String variable = StringUtil.replaceNonAlphanumericByUnderscores(property).toUpperCase();
                environment.put(variable, value);
            }
        });
        return environment;
    }

    private EnvVar getEnvVarByKey(String key, Container container) {
        return container.getEnv().stream().filter(env -> StringUtils.equals(key, env.getName())).findFirst().orElse(null);
    }

    private Map<String, String> enrichProperties(Map<String, String> properties, Deployment deployment,
            boolean isQuarkusRuntime) {
        // mount path x volume
        Map<String, CustomVolume> volumes = new HashMap<>();

        // the idea of the 'output' is that if you have quarkus.some.property.key=secret::/path
        // then it is turned into quarkus.some.property.key=path
        Map<String, String> output = new HashMap<>();

        for (Entry<String, String> entry : properties.entrySet()) {
            String propertyValue = entry.getValue();
            if (isResource(propertyValue)) {
                String path = entry.getValue().replace(RESOURCE_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String configMapName = normalizeConfigMapName(mountPath, filename);

                // Update config map
                createOrUpdateConfigMap(configMapName, filename, getFileContent(path));

                // Add the volume
                if (!volumes.containsKey(mountPath)) {
                    volumes.put(mountPath, new CustomVolume(configMapName, "", CONFIG_MAP));
                }

                propertyValue = joinMountPathAndFileName(mountPath, filename);
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
                String configMapName = normalizeConfigMapName(mountPath, fileNameNormalized);

                // Update config map
                createOrUpdateConfigMap(configMapName, fileNameNormalized, getFileContent(fileName));
                propertyValue = joinMountPathAndFileName(mountPath, fileNameNormalized);
                // Add the volume
                if (!volumes.containsKey(propertyValue)) {
                    volumes.put(propertyValue, new CustomVolume(configMapName, fileNameNormalized, CONFIG_MAP));
                }
            } else if (propertyValue.startsWith(SECRET_WITH_DESTINATION_PREFIX)) {
                String path = entry.getValue().replace(SECRET_WITH_DESTINATION_PREFIX, StringUtils.EMPTY);
                int separatorIdx = path.lastIndexOf(DESTINATION_TO_FILENAME_SEPARATOR);
                final String mountPath = path.substring(0, separatorIdx);
                final String filename = path.substring(separatorIdx + 1);
                String secretName = normalizeConfigMapName(mountPath, filename);

                // Push secret file
                propertyValue = joinMountPathAndFileName(mountPath, filename);
                String filePath = Files.exists(Path.of(propertyValue)) ? propertyValue : getFilePath(SLASH + filename);
                doCreateSecretFromFile(secretName, filePath);
                volumes.putIfAbsent(mountPath, new CustomVolume(secretName, "", SECRET));
            } else if (isSecret(propertyValue)) {
                String path = entry.getValue().replace(SECRET_PREFIX, StringUtils.EMPTY);
                String mountPath = getMountPath(path);
                String filename = getFileName(path);
                String secretName = normalizeConfigMapName(mountPath, filename);

                // Push secret file
                doCreateSecretFromFile(secretName, getFilePath(path));
                volumes.put(mountPath, new CustomVolume(secretName, "", SECRET));

                propertyValue = joinMountPathAndFileName(mountPath, filename);
            } else if (isQuarkusRuntime && isMountSecret(propertyValue)) {
                // mount existing secret
                var secretNameToMountPath = getMountSecret(propertyValue);
                var secretName = secretNameToMountPath.key();
                var mountPath = secretNameToMountPath.value();
                volumes.put(mountPath, new CustomVolume(secretName, "", SECRET));

                propertyValue = mountPath;
            } else if (isQuarkusRuntime && isMountConfigMap(propertyValue)) {
                // mount existing configmap
                var configMapNameToMountPath = getMountConfigMap(propertyValue);
                var configMapName = configMapNameToMountPath.key();
                var mountPath = configMapNameToMountPath.value();
                volumes.put(mountPath, new CustomVolume(configMapName, "", CONFIG_MAP));

                propertyValue = mountPath;
            } else if (isQuarkusRuntime && isAnnotatedConfigMap(propertyValue)) {
                var annotatedConfigMap = getAnnotatedConfigMap(propertyValue);
                var configMapName = annotatedConfigMap.key();
                var annotationName = annotatedConfigMap.value().key();
                var annotationVal = annotatedConfigMap.value().value();
                addAnnotatedConfigMap(configMapName, annotationName, annotationVal);

                // no sensible value is expected, just assign something
                propertyValue = annotationVal;
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

    private void createOrUpdateConfigMap(String configMapName, String key, String value) {
        final var configMaps = client.configMaps();
        if (configMaps.withName(configMapName).get() != null) {
            // update existing config map by adding new file
            configMaps.withName(configMapName)
                    .edit(configMap -> {
                        configMap.getData().put(key, value);
                        return configMap;
                    });
        } else {
            // create new one
            configMaps.resource(new ConfigMapBuilder().withNewMetadata().withName(configMapName).endMetadata()
                    .addToData(key, value).build()).unlock().create();
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

    private String normalizeConfigMapName(String mountPath, String fileName) {
        // /some/mount/path/file-name => some-mount-path-file-name
        var newName = StringUtils.removeStart(joinMountPathAndFileName(mountPath, fileName), SLASH)
                .replaceAll(Pattern.quote("."), "-")
                .replaceAll(SLASH, "-");
        if (newName.length() > SPECS_SECRET_NAME_LIMIT) {
            newName = newName.substring(newName.length() - SPECS_SECRET_NAME_LIMIT);
            while (newName.startsWith("-")) {
                // must not start with '-something' as it's considered to be a flag
                newName = newName.substring(1);
            }
        }
        return newName;
    }

    private static String joinMountPathAndFileName(String mountPath, String fileName) {
        if (!mountPath.endsWith(SLASH)) {
            // /some/mount/path => /some/mount/path/
            mountPath += SLASH;
        }
        return mountPath + fileName;
    }

    private boolean isResourceWithDestinationPath(String key) {
        return key.startsWith(RESOURCE_WITH_DESTINATION_PREFIX);
    }

    private boolean isResource(String key) {
        return key.startsWith(RESOURCE_PREFIX);
    }

    private boolean isSecret(String key) {
        return key.startsWith(SECRET_PREFIX);
    }

    private boolean hasImageStreamTags(ImageStream is) {
        return !client.imageStreams().withName(is.getMetadata().getName()).get().getStatus().getTags().isEmpty();
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
        NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata> load = client
                .load(new ByteArrayInputStream(template.getBytes()));
        return load.items();
    }

    private String generateRandomProjectName() {
        return ThreadLocalRandom.current().ints(PROJECT_NAME_SIZE, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static boolean isQuarkusProperty(String propertyKey) {
        return propertyKey.startsWith("quarkus.");
    }
}
