package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.MavenUtils.BATCH_MODE;
import static io.quarkus.test.utils.MavenUtils.DISPLAY_VERSION;
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

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionOpenShiftQuarkusApplicationManagedResource
        extends OpenShiftQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String USING_EXTENSION_PROFILE = "-Pdeploy-to-openshift-using-extension";
    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String QUARKUS_PLUGIN_EXPOSE = "-Dquarkus.openshift.expose=true";
    private static final String QUARKUS_PLUGIN_ROUTE_EXPOSE = "-Dquarkus.openshift.route.expose=true";
    private static final String QUARKUS_CONTAINER_NAME = "quarkus.application.name";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_OPENSHIFT_ENV_VARS = "quarkus.openshift.env.vars.";
    private static final String QUARKUS_OPENSHIFT_LABELS = "quarkus.openshift.labels.";
    private static final String QUARKUS_KNATIVE_ENV_VARS = "quarkus.knative.env.vars.";
    private static final String QUARKUS_KNATIVE_LABELS = "quarkus.knative.labels.";
    private static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    private static final String KNATIVE = "knative";
    private static final Path RESOURCES_FOLDER = Paths.get("src", "main", "resources", "application.properties");

    public ExtensionOpenShiftQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
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
            fail("Custom source classes or forced dependencies is not supported by `UsingOpenShiftExtension`");
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

        PropertiesUtils.fromMap(buildProperties, RESOURCES_FOLDER);
        model.createSnapshotOfBuildProperties();
    }

    private void deployProjectUsingMavenCommand() {
        installParentPomsIfNeeded();

        String namespace = client.project();

        List<String> args = mvnCommand(model.getContext());
        args.addAll(Arrays.asList(USING_EXTENSION_PROFILE, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL,
                QUARKUS_PLUGIN_DEPLOY, QUARKUS_PLUGIN_EXPOSE, QUARKUS_PLUGIN_ROUTE_EXPOSE,
                SKIP_TESTS, SKIP_ITS, SKIP_CHECKSTYLE));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
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

    private String withLabelsForWatching() {
        return withLabels(OpenShiftClient.LABEL_TO_WATCH_FOR_LOGS, model.getContext().getOwner().getName());
    }

    private String withLabelsForScenarioId() {
        return withLabels(OpenShiftClient.LABEL_SCENARIO_ID, client.getScenarioId());
    }

    private String withLabels(String label, String value) {
        String property = QUARKUS_OPENSHIFT_LABELS;
        if (isKnativeDeployment()) {
            property = QUARKUS_KNATIVE_LABELS;
        }

        return withProperty(property + label, value);
    }

    private String withContainerName() {
        return withProperty(QUARKUS_CONTAINER_NAME, model.getContext().getName());
    }

    private String withContainerImageGroup(String namespace) {
        return withProperty(QUARKUS_CONTAINER_IMAGE_GROUP, namespace);
    }

    private String withKubernetesClientNamespace(String namespace) {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_NAMESPACE, namespace);
    }

    private String withKubernetesClientTrustCerts() {
        return withProperty(QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS, Boolean.TRUE.toString());
    }

    private void withEnvVars(List<String> args, Map<String, String> envVars) {
        String property = QUARKUS_OPENSHIFT_ENV_VARS;
        if (isKnativeDeployment()) {
            property = QUARKUS_KNATIVE_ENV_VARS;
        }

        for (Entry<String, String> envVar : envVars.entrySet()) {
            String envVarKey = envVar.getKey().replaceAll(Pattern.quote("."), "-");
            args.add(withProperty(property + envVarKey, envVar.getValue()));
        }
    }

    private boolean isKnativeDeployment() {
        return KNATIVE.equals(model.getComputedProperty(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET));
    }

    private void cloneProjectToServiceAppFolder() {
        FileUtils.copyCurrentDirectoryTo(model.getContext().getServiceFolder());
    }

}
