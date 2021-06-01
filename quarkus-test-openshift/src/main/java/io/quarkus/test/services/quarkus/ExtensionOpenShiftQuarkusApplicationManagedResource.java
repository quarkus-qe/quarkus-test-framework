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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;

public class ExtensionOpenShiftQuarkusApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String USING_EXTENSION_PROFILE = "-Pdeploy-to-openshift-using-extension";
    private static final String QUARKUS_PLUGIN_DEPLOY = "-Dquarkus.kubernetes.deploy=true";
    private static final String QUARKUS_PLUGIN_EXPOSE = "-Dquarkus.openshift.expose=true";
    private static final String QUARKUS_CONTAINER_NAME = "quarkus.application.name";
    private static final String QUARKUS_KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";
    private static final String QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS = "quarkus.kubernetes-client.trust-certs";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_OPENSHIFT_ENV_VARS = "quarkus.openshift.env.vars.";
    private static final String QUARKUS_OPENSHIFT_LABELS = "quarkus.openshift.labels.";

    private static final String QUARKUS_PROPERTY_PREFIX = "quarkus";

    private static final String APPLICATION_PROPERTIES_PATH = "src/main/resources/application.properties";

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

        if (model.isSelectedAppClasses()) {
            fail("Custom source classes as @QuarkusApplication(classes = ...) is not supported by `UsingOpenShiftExtension`");
        }
    }

    protected void withAdditionalArguments(List<String> args) {

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
        installParentPomsIfNeeded();

        String namespace = client.project();

        List<String> args = mvnCommand(model.getContext());
        args.addAll(Arrays.asList(USING_EXTENSION_PROFILE, BATCH_MODE, DISPLAY_VERSION, PACKAGE_GOAL,
                QUARKUS_PLUGIN_DEPLOY, QUARKUS_PLUGIN_EXPOSE, SKIP_TESTS, SKIP_ITS, SKIP_CHECKSTYLE));
        args.add(withContainerName());
        args.add(withKubernetesClientNamespace(namespace));
        args.add(withKubernetesClientTrustCerts());
        args.add(withContainerImageGroup(namespace));
        args.add(withLabelsForWatching());
        withQuarkusProperties(args);
        withEnvVars(args, model.getContext().getOwner().getProperties());
        withAdditionalArguments(args);

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

    private String withContainerName() {
        return withProperty(QUARKUS_CONTAINER_NAME, model.getContext().getName());
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
