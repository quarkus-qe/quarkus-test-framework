package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.DockerUtils.CONTAINER_REGISTRY_URL_PROPERTY;
import static io.quarkus.test.utils.MavenUtils.withProperty;
import static io.quarkus.test.utils.MavenUtils.withQuarkusProfile;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MavenUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionKubernetesQuarkusApplicationManagedResource
        extends KubernetesQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String USING_SERVICE_TYPE_NODE_PORT = "-Dquarkus.kubernetes.service-type=NodePort";
    private static final String QUARKUS_CONTAINER_NAME = "quarkus.application.name";
    private static final String QUARKUS_CONTAINER_IMAGE_REGISTRY = "quarkus.container-image.registry";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_KUBERNETES_ENV_VARS = "quarkus.kubernetes.env.vars.";
    private static final String QUARKUS_KUBERNETES_LABELS = "quarkus.kubernetes.labels.";
    private static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources", "application.properties");

    public ExtensionKubernetesQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void doInit() {
        cloneProjectToServiceAppFolder();
        deployProjectUsingMavenCommand();
    }

    @Override
    protected void doUpdate() {
        client.applyServicePropertiesUsingDeploymentConfig(model.getContext().getOwner());
    }

    @Override
    public boolean needsBuildArtifact() {
        return false;
    }

    @Override
    public void validate() {
        super.validate();

        if (model.requiresCustomBuild()) {
            fail("Custom source classes or forced dependencies is not supported by `UsingKubernetesExtension`");
        }
    }

    protected void withAdditionalArguments(List<String> args) {

    }

    private void copyBuildPropertiesIntoAppFolder(QuarkusMavenPluginBuildHelper quarkusMvnPluginHelper) {
        quarkusMvnPluginHelper.withProjectDirectoryCustomizer(projectDirectory -> {
            // always copy service properties to app folder as system properties are not propagated to OpenShift
            var runtimeSvcProperties = model.getContext().getOwner().getProperties().entrySet().stream()
                    .filter(e -> !model.isBuildProperty(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!runtimeSvcProperties.isEmpty()) {
                model.setCustomBuildRequired();
                Path appPropertiesPath = projectDirectory.resolve(RESOURCES_FOLDER);

                var allProperties = new HashMap<String, String>();
                Path computedPropertiesPath = model.getComputedApplicationProperties();
                if (Files.exists(computedPropertiesPath)) {
                    allProperties.putAll(PropertiesUtils.toMap(computedPropertiesPath));
                }
                allProperties.putAll(runtimeSvcProperties);

                PropertiesUtils.fromMap(allProperties, appPropertiesPath);
                model.createSnapshotOfBuildProperties();
            }
        });
    }

    private void deployProjectUsingMavenCommand() {
        String namespace = client.namespace();

        // deploy-to-kubernetes-using-extension used to activate profile that wasn't active during initial build
        // so we need to make sure that extension is always present in case users relied on that
        var openshiftExtension = List.of(new Dependency("io.quarkus", "quarkus-kubernetes", null));
        var quarkusMvnPluginHelper = new QuarkusMavenPluginBuildHelper(this.model,
                this.model.getTargetFolderForLocalArtifacts(), this.model.getArtifactSuffix(), openshiftExtension);
        copyBuildPropertiesIntoAppFolder(quarkusMvnPluginHelper);
        List<String> args = new ArrayList<>();
        MavenUtils.withProperties(args);
        propagateContainerRegistryIfSet(args);
        args.add(QUARKUS_PLUGIN_DEPLOY);
        args.add(USING_SERVICE_TYPE_NODE_PORT);
        args.add(withQuarkusProfile(model.getContext()));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withLabelsForWatching());
        args.add(withLabelsForScenarioId());

        withAdditionalArguments(args);

        withEnvVars(args, model.getContext().getOwner().getProperties());

        quarkusMvnPluginHelper.buildOrReuseArtifact(new HashSet<>(args));
    }

    private void propagateContainerRegistryIfSet(List<String> args) {
        String containerRegistry = System.getProperty(CONTAINER_REGISTRY_URL_PROPERTY);
        if (StringUtils.isNotEmpty(containerRegistry)) {
            int lastSlash = containerRegistry.lastIndexOf("/");
            String registryHost = containerRegistry.substring(0, lastSlash);
            String registryGroup = containerRegistry.substring(lastSlash + 1);
            args.add(withProperty(QUARKUS_CONTAINER_IMAGE_REGISTRY, registryHost));
            args.add(withProperty(QUARKUS_CONTAINER_IMAGE_GROUP, registryGroup));
        }
    }

    private String withLabelsForWatching() {
        return withLabels(KubectlClient.LABEL_TO_WATCH_FOR_LOGS, model.getContext().getOwner().getName());
    }

    private String withLabelsForScenarioId() {
        return withLabels(KubectlClient.LABEL_SCENARIO_ID, client.getScenarioId());
    }

    private String withLabels(String label, String value) {
        return withProperty(QUARKUS_KUBERNETES_LABELS + label, value);
    }

    private String withContainerName() {
        return withProperty(QUARKUS_CONTAINER_NAME, model.getContext().getName());
    }

    private String withKubernetesClientNamespace(String namespace) {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_NAMESPACE, namespace);
    }

    private String withKubernetesClientTrustCerts() {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS, Boolean.TRUE.toString());
    }

    private void withEnvVars(List<String> args, Map<String, String> envVars) {
        for (Entry<String, String> envVar : envVars.entrySet()) {
            String envVarKey = envVar.getKey().replaceAll(Pattern.quote("."), "-");
            args.add(withProperty(QUARKUS_KUBERNETES_ENV_VARS + envVarKey, envVar.getValue()));
        }
    }

    private void cloneProjectToServiceAppFolder() {
        FileUtils.copyCurrentDirectoryTo(model.getContext().getServiceFolder());
    }

}
