package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.DockerUtils.CONTAINER_REGISTRY_URL_PROPERTY;
import static io.quarkus.test.utils.MavenUtils.BATCH_MODE;
import static io.quarkus.test.utils.MavenUtils.DISPLAY_VERSION;
import static io.quarkus.test.utils.MavenUtils.ENSURE_QUARKUS_BUILD;
import static io.quarkus.test.utils.MavenUtils.PACKAGE_GOAL;
import static io.quarkus.test.utils.MavenUtils.SKIP_CHECKSTYLE;
import static io.quarkus.test.utils.MavenUtils.SKIP_ITS;
import static io.quarkus.test.utils.MavenUtils.SKIP_TESTS;
import static io.quarkus.test.utils.MavenUtils.installParentPomsIfNeeded;
import static io.quarkus.test.utils.MavenUtils.mvnCommand;
import static io.quarkus.test.utils.MavenUtils.withProperty;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionKubernetesQuarkusApplicationManagedResource
        extends KubernetesQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String USING_EXTENSION_PROFILE = "-Pdeploy-to-kubernetes-using-extension";
    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String QUARKUS_PLUGIN_INGRESS_EXPOSE = "-Dquarkus.kubernetes.ingress.expose=true";
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
        copyBuildPropertiesIntoAppFolder();
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

    private void copyBuildPropertiesIntoAppFolder() {
        Map<String, String> buildProperties = model.getBuildProperties();
        if (buildProperties.isEmpty()) {
            return;
        }

        Path applicationPropertiesPath = model.getComputedApplicationProperties();
        if (Files.exists(applicationPropertiesPath)) {
            buildProperties.putAll(PropertiesUtils.toMap(applicationPropertiesPath));
        }

        PropertiesUtils.fromMap(buildProperties, getContext().getServiceFolder().resolve(RESOURCES_FOLDER));
        model.createSnapshotOfBuildProperties();
    }

    private void deployProjectUsingMavenCommand() {
        installParentPomsIfNeeded();

        String namespace = client.namespace();

        List<String> args = mvnCommand(model.getContext());
        args.addAll(Arrays.asList(USING_EXTENSION_PROFILE, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL,
                QUARKUS_PLUGIN_DEPLOY, QUARKUS_PLUGIN_INGRESS_EXPOSE,
                SKIP_TESTS, SKIP_ITS, SKIP_CHECKSTYLE, ENSURE_QUARKUS_BUILD));
        propagateContainerRegistryIfSet(args);
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withLabelsForWatching());
        args.add(withLabelsForScenarioId());
        withEnvVars(args, model.getContext().getOwner().getProperties());
        withAdditionalArguments(args);

        try {
            new Command(args).onDirectory(model.getContext().getServiceFolder()).runAndWait();
        } catch (Exception e) {
            fail("Failed to run maven command. Caused by " + e.getMessage());
        }
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
