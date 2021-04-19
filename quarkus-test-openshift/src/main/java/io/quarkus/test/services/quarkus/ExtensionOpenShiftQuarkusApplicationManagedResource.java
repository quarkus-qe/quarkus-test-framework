package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionOpenShiftQuarkusApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String MVN_COMMAND = "mvn";
    private static final String PACKAGE_GOAL = "package";
    private static final String USING_EXTENSION_PROFILE = "-Pdeploy-to-openshift-using-extension";
    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String QUARKUS_PLUGIN_EXPOSE = "-Dquarkus.openshift.expose=true";
    private static final String MVN_REPOSITORY_LOCAL = "maven.repo.local";
    private static final String SKIP_TESTS = "-DskipTests=true";
    private static final String SKIP_ITS = "-DskipITs=true";
    private static final String BATCH_MODE = "-B";
    private static final String DISPLAY_VERSION = "-V";
    private static final String SKIP_CHECKSTYLE = "-Dcheckstyle.skip";
    private static final String QUARKUS_CONTAINER_NAME = "quarkus.application.name";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_OPENSHIFT_ENV_VARS = "quarkus.openshift.env.vars.";
    private static final String QUARKUS_OPENSHIFT_LABELS = "quarkus.openshift.labels.";
    private static final String QUARKUS_OPENSHIFT_BUILD_STRATEGY = "quarkus.openshift.build-strategy";

    private static final String QUARKUS_PROPERTY_PREFIX = "quarkus";

    private static final String DOCKERFILE_SOURCE_FOLDER = "src/main/docker";
    private static final String APPLICATION_PROPERTIES_PATH = "src/main/resources/application.properties";
    private static final String DOCKER = "docker";

    public ExtensionOpenShiftQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
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

        if (model.isSelectedAppClasses()) {
            fail("Custom source classes as @QuarkusApplication(classes = ...) is not supported by `UsingOpenShiftExtension`");
        }
    }

    private void copyBuildPropertiesIntoAppFolder() {
        Map<String, String> buildProperties = model.getBuildProperties();
        if (buildProperties.isEmpty()) {
            return;
        }

        Path applicationPropertiesPath = model.getContext().getServiceFolder().resolve(APPLICATION_PROPERTIES_PATH);
        if (Files.exists(applicationPropertiesPath)) {
            buildProperties.putAll(PropertiesUtils.toMap(applicationPropertiesPath));
        }

        PropertiesUtils.fromMap(buildProperties, applicationPropertiesPath);
        model.createSnapshotOfBuildProperties();
    }

    private void deployProjectUsingMavenCommand() {
        String namespace = client.project();

        List<String> args = new ArrayList<>(
                Arrays.asList(MVN_COMMAND, USING_EXTENSION_PROFILE, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL,
                        QUARKUS_PLUGIN_DEPLOY, QUARKUS_PLUGIN_EXPOSE, SKIP_TESTS, SKIP_ITS, SKIP_CHECKSTYLE));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
        args.add(withLabelsForWatching());
        withBuildStrategy(args);
        withQuarkusProperties(args);
        withMavenRepositoryLocalIfSet(args);
        withEnvVars(args, model.getContext().getOwner().getProperties());

        try {
            new Command(args).onDirectory(model.getContext().getServiceFolder()).runAndWait();
        } catch (Exception e) {
            fail("Failed to run maven command. Caused by " + e.getMessage());
        }
    }

    private String withLabelsForWatching() {
        return withProperty(QUARKUS_OPENSHIFT_LABELS + OpenShiftClient.LABEL_TO_WATCH_FOR_LOGS,
                model.getContext().getOwner().getName());
    }

    private void withBuildStrategy(List<String> args) {
        if (isDockerBuildStrategySet()) {
            copyDockerfileToSources();

            args.add(withProperty(QUARKUS_OPENSHIFT_BUILD_STRATEGY, DOCKER));
        }
    }

    private void copyDockerfileToSources() {
        Path dockerfileTarget = model.getContext().getServiceFolder().resolve(DOCKERFILE_SOURCE_FOLDER);
        if (!Files.exists(dockerfileTarget)) {
            FileUtils.createDirectory(dockerfileTarget);
        }

        String dockerfileName = DockerUtils.getDockerfile(model.getLaunchMode());
        if (!Files.exists(dockerfileTarget.resolve(dockerfileTarget))) {
            String dockerFileContent = FileUtils.loadFile(DockerUtils.getDockerfile(model.getLaunchMode()))
                    .replaceAll(quote("${ARTIFACT_PARENT}"), "target");
            FileUtils.copyContentTo(dockerFileContent,
                    model.getContext().getServiceFolder().resolve(DOCKERFILE_SOURCE_FOLDER + dockerfileName));
        }
    }

    private boolean isDockerBuildStrategySet() {
        OpenShiftScenario annotation = model.getContext().getTestContext().getRequiredTestClass()
                .getAnnotation(OpenShiftScenario.class);
        return annotation.deployment() == OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy;
    }

    private String withContainerName() {
        return withProperty(QUARKUS_CONTAINER_NAME, model.getContext().getName());
    }

    private void withMavenRepositoryLocalIfSet(List<String> args) {
        String mvnRepositoryPath = System.getProperty(MVN_REPOSITORY_LOCAL);
        if (mvnRepositoryPath != null) {
            args.add(withProperty(MVN_REPOSITORY_LOCAL, mvnRepositoryPath));
        }
    }

    private void withQuarkusProperties(List<String> args) {
        System.getProperties().entrySet().stream()
                .filter(isQuarkusProperty().and(propertyValueIsNotEmpty()))
                .forEach(property -> {
                    String key = (String) property.getKey();
                    String value = (String) property.getValue();
                    args.add(withProperty(key, value));
                });
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
        for (Entry<String, String> envVar : envVars.entrySet()) {
            String envVarKey = envVar.getKey().replaceAll(Pattern.quote("."), "-");
            args.add(withProperty(QUARKUS_OPENSHIFT_ENV_VARS + envVarKey, envVar.getValue()));
        }
    }

    private String withProperty(String property, String value) {
        return String.format("-D%s=%s", property, value);
    }

    private Predicate<Entry<Object, Object>> propertyValueIsNotEmpty() {
        return property -> StringUtils.isNotEmpty((String) property.getValue());
    }

    private Predicate<Entry<Object, Object>> isQuarkusProperty() {
        return property -> StringUtils.startsWith((String) property.getKey(), QUARKUS_PROPERTY_PREFIX);
    }

    private void cloneProjectToServiceAppFolder() {
        FileUtils.copyCurrentDirectoryTo(model.getContext().getServiceFolder());
    }

}
