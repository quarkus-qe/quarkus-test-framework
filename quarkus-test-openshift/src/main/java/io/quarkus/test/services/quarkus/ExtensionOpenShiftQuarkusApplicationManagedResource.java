package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_NATIVE_S2I;
import static io.quarkus.test.utils.MavenUtils.withProperty;
import static io.quarkus.test.utils.MavenUtils.withQuarkusProfile;

import java.io.IOException;
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

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MavenUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionOpenShiftQuarkusApplicationManagedResource
        extends OpenShiftQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String QUARKUS_PLUGIN_ROUTE_EXPOSE = "-Dquarkus.openshift.route.expose=true";
    private static final String QUARKUS_CONTAINER_NAME = "quarkus.application.name";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_OPENSHIFT_ENV_VARS = "quarkus.openshift.env.vars.";
    private static final String QUARKUS_OPENSHIFT_LABELS = "quarkus.openshift.labels.";
    private static final String QUARKUS_OPENSHIFT_BASE_JAVA_IMAGE = "quarkus.openshift.base-jvm-image";
    private static final String QUARKUS_OPENSHIFT_BASE_NATIVE_IMAGE = "quarkus.openshift.base-native-image";
    private static final String QUARKUS_KNATIVE_ENV_VARS = "quarkus.knative.env.vars.";
    private static final String QUARKUS_KNATIVE_LABELS = "quarkus.knative.labels.";
    private static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
    private static final String OPENSHIFT = "openshift";
    private static final String KNATIVE = "knative";
    private static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET_OPENSHIFT = String.format("-D%s=%s",
            QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, OPENSHIFT);
    private static final Path APPLICATION_PROPERTIES_PATH = Paths.get("src", "main", "resources", "application.properties");

    public ExtensionOpenShiftQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void doInit() {
        cloneProjectToServiceAppFolder();
        deployProjectUsingMavenCommand();
        exposeManagementRoute();
    }

    /*
     * management routes are not exposed by extension,
     * see: https://github.com/quarkusio/quarkus/issues/32269
     */
    private void exposeManagementRoute() {
        if (model.useSeparateManagementInterface()) {
            final String app = model.getContext().getOwner().getName();
            final String routeName = app + "-management";
            final int port = model.getManagementPort();
            client.createService(app, routeName, port);
            client.expose(routeName, routeName, port);
        }
    }

    @Override
    protected void doUpdate() {
        client.applyServicePropertiesToDeployment(model.getContext().getOwner());
    }

    @Override
    public boolean needsBuildArtifact() {
        return false;
    }

    protected void withAdditionalArguments(List<String> args, QuarkusMavenPluginBuildHelper quarkusMvnPluginHelper) {

    }

    private void copyServicePropertiesIntoAppFolder(QuarkusMavenPluginBuildHelper quarkusMvnPluginHelper) {
        quarkusMvnPluginHelper.withProjectDirectoryCustomizer(projectDirectory -> {
            // always copy service properties to app folder as system properties are not propagated to OpenShift
            var runtimeSvcProperties = model.getContext().getOwner().getProperties().entrySet().stream()
                    .filter(e -> !model.isBuildProperty(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!runtimeSvcProperties.isEmpty()) {
                model.setCustomBuildRequired();
                Path appPropertiesPath = projectDirectory.resolve(APPLICATION_PROPERTIES_PATH);
                createApplicationPropertiesIfNotExists(appPropertiesPath);

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

    private static void createApplicationPropertiesIfNotExists(Path appPropertiesPath) {
        if (!Files.exists(appPropertiesPath)) {
            if (!Files.exists(appPropertiesPath.getParent())) {
                appPropertiesPath.getParent().toFile().mkdirs();
            }
            try {
                appPropertiesPath.toFile().createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create application properties file at path: '%s'".formatted(appPropertiesPath), e);
            }
        }
    }

    private void deployProjectUsingMavenCommand() {
        String namespace = client.project();

        // deploy-to-openshift-using-extension used to activate profile that wasn't active during initial build
        // so we need to make sure that extension is always present in case users relied on that
        var openshiftExtension = List.of(new Dependency("io.quarkus", "quarkus-openshift", null));
        var quarkusMvnPluginHelper = new QuarkusMavenPluginBuildHelper(this.model,
                this.model.getTargetFolderForLocalArtifacts(), this.model.getArtifactSuffix(), openshiftExtension);
        copyServicePropertiesIntoAppFolder(quarkusMvnPluginHelper);
        List<String> args = new ArrayList<>();
        MavenUtils.withProperties(args);
        args.add(QUARKUS_PLUGIN_DEPLOY);
        args.add(QUARKUS_PLUGIN_ROUTE_EXPOSE);
        args.add(withQuarkusProfile(model.getContext()));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
        args.add(withLabelsForWatching());
        args.add(withLabelsForScenarioId());

        withDeploymentTarget(args);
        withEnvVars(args);
        withBaseImageProperties(args);
        withAdditionalArguments(args, quarkusMvnPluginHelper);

        quarkusMvnPluginHelper.buildOrReuseArtifact(new HashSet<>(args));
    }

    private void withDeploymentTarget(List<String> args) {
        final String deploymentTarget = model.getContext().getOwner().getProperty(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, null);
        if (deploymentTarget == null || deploymentTarget.isBlank()) {
            args.add(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET_OPENSHIFT);
        }
    }

    private void withBaseImageProperties(List<String> args) {
        // Resolve s2i property
        boolean isNativeTest = isNativeTest();
        PropertyLookup s2iImageProperty = isNativeTest ? QUARKUS_NATIVE_S2I : QUARKUS_JVM_S2I;
        String baseImage = model.getContext().getOwner().getProperty(s2iImageProperty.getPropertyKey())
                .orElseGet(() -> s2iImageProperty.get(model.getContext()));

        // Set S2i property
        args.add(withProperty(s2iImageProperty.getPropertyKey(), baseImage));

        // If S2i is set and the openshift is not, we need to propagate it.
        String openShiftImageProperty = isNativeTest ? QUARKUS_OPENSHIFT_BASE_NATIVE_IMAGE : QUARKUS_OPENSHIFT_BASE_JAVA_IMAGE;
        if (model.getContext().getOwner().getProperty(openShiftImageProperty).isEmpty()) {
            args.add(withProperty(openShiftImageProperty, baseImage));
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

    private void withEnvVars(List<String> args) {
        Map<String, String> envVars = model.getContext().getOwner().getProperties();
        String property = QUARKUS_OPENSHIFT_ENV_VARS;
        if (isKnativeDeployment()) {
            property = QUARKUS_KNATIVE_ENV_VARS;
        }
        for (Entry<String, String> envVar : envVars.entrySet()) {
            if (requiredByExtension(envVar.getKey())) {
                args.add(withProperty(envVar.getKey(), envVar.getValue()));
            } else {
                String envVarKey = envVar.getKey().replaceAll(Pattern.quote("."), "-");
                args.add(withProperty(property + envVarKey, envVar.getValue()));
            }
        }
    }

    private static boolean requiredByExtension(String parameter) {
        return parameter.startsWith("quarkus.management.") || parameter.endsWith(".grpc-action");
    }

    private boolean isKnativeDeployment() {
        return KNATIVE.equals(model.getContext().getOwner().getProperty(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, null));
    }

    protected void cloneProjectToServiceAppFolder() {
        FileUtils.copyCurrentDirectoryTo(model.getContext().getServiceFolder());
    }
}
