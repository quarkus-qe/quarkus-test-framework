package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_NATIVE_S2I;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionOpenShiftQuarkusApplicationManagedResource
        extends OpenShiftQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String USING_EXTENSION_PROFILE = "-Pdeploy-to-openshift-using-extension";
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
        copyComputedPropertiesIntoAppFolder();
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

    @Override
    public void validate() {
        super.validate();

        if (model.requiresCustomBuild()) {
            fail("Custom source classes or forced dependencies is not supported by `UsingOpenShiftExtension`");
        }
    }

    protected void withAdditionalArguments(List<String> args) {

    }

    private void copyComputedPropertiesIntoAppFolder() {
        // always copy computed properties to app folder as system properties are not propagated to OpenShift
        Path computedPropertiesPath = model.getComputedApplicationProperties();
        if (Files.exists(computedPropertiesPath)) {
            var computedProperties = PropertiesUtils.toMap(computedPropertiesPath);
            if (!computedProperties.isEmpty()) {
                Path appPropertiesPath = model.getApplicationFolder().resolve(APPLICATION_PROPERTIES_PATH);
                createApplicationPropertiesIfNotExists(appPropertiesPath);
                PropertiesUtils.fromMap(computedProperties, appPropertiesPath);
            }
        }
        model.createSnapshotOfBuildProperties();
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
        installParentPomsIfNeeded();

        String namespace = client.project();

        List<String> args = mvnCommand(model.getContext());
        args.addAll(Arrays.asList(USING_EXTENSION_PROFILE, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL,
                QUARKUS_PLUGIN_DEPLOY, QUARKUS_PLUGIN_ROUTE_EXPOSE, SKIP_TESTS, SKIP_ITS, SKIP_CHECKSTYLE,
                ENSURE_QUARKUS_BUILD));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
        args.add(withLabelsForWatching());
        args.add(withLabelsForScenarioId());

        withDeploymentTarget(args);
        withEnvVars(args);
        withBaseImageProperties(args);
        withAdditionalArguments(args);

        try {
            new Command(args).onDirectory(model.getApplicationFolder()).runAndWait();
        } catch (Exception e) {
            fail("Failed to run maven command. Caused by " + e.getMessage());
        }
    }

    private void withDeploymentTarget(List<String> args) {
        final String deploymentTarget = model.getComputedProperty(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET);
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

    private String withJarFileName() {
        return withProperty("quarkus.openshift.jar-file-name", "quarkus-run.jar");
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
        return KNATIVE.equals(model.getComputedProperty(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET));
    }

    protected void cloneProjectToServiceAppFolder() {
        FileUtils.copyCurrentDirectoryTo(model.getContext().getServiceFolder());
    }
}
