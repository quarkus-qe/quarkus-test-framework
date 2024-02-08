package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryQuarkusApplicationManagedResourceBuilder.QUARKUS_PLATFORM_GROUP_ID_PROPERTY;
import static io.quarkus.test.services.quarkus.GitRepositoryQuarkusApplicationManagedResourceBuilder.QUARKUS_PLATFORM_VERSION_PROPERTY;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static java.util.regex.Pattern.quote;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshotCondition;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<GitRepositoryQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "/quarkus-s2i-source-build-template.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";
    private static final String INTERNAL_MAVEN_REPOSITORY_PROPERTY = "${internal.s2i.maven.remote.repository}";
    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY = new PropertyLookup("s2i.maven.remote.repository");
    private static final PropertyLookup QUARKUS_NATIVE_S2I_FROM_SRC = new PropertyLookup("s2i.openshift.base-native-image",
            "quay.io/quarkus/ubi-quarkus-graalvmce-s2i:jdk-17");

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void validate() {
        super.validate();
        if (model.isDevMode()) {
            Assertions.fail("DEV mode is not supported when using GIT repositories on OpenShift deployments");
        }

        if (DisabledOnQuarkusSnapshotCondition.isQuarkusSnapshotVersion()
                && StringUtils.isEmpty(MAVEN_REMOTE_REPOSITORY.get(model.getContext()))) {
            Assertions.fail("s2i can't use the Quarkus 999-SNAPSHOT version if not Maven repository has been provided");
        }
    }

    @Override
    protected String getDefaultTemplate() {
        return QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME;
    }

    /**
     * - Express parameters in S2I build application (resources/quarkus-s2i-source-build-template.yml).
     * - Add environment variables to deployment config in S2I build application
     * (resources/quarkus-s2i-source-build-template.yml).
     * - Enrich deployment config for purposes of the test suite in S2I build application
     * (resources/quarkus-s2i-source-build-template.yml).
     * - Apply the resulting OpenShift yml file.
     * - Wait for build config to have a complete build.
     * - Wait for deployment to be ready.
     */
    @Override
    protected void doInit() {
        createMavenSettings();
        super.doInit();
        client.followBuildConfigLogs(model.getContext().getName());
    }

    @Override
    protected boolean needsBuildArtifact() {
        return false;
    }

    protected String replaceDeploymentContent(String content) {
        String quarkusPlatformVersion = QuarkusProperties.getVersion();
        String quarkusS2iBaseImage = getQuarkusS2iBaseImage();
        String mavenArgs = model.getMavenArgsWithVersion();

        return content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${QUARKUS_S2I_BUILDER_IMAGE}"), quarkusS2iBaseImage)
                .replaceAll(quote("${GIT_URI}"), model.getGitRepository())
                .replaceAll(quote("${GIT_REF}"), model.getGitBranch())
                .replaceAll(quote("${CONTEXT_DIR}"), model.getContextDir())
                .replaceAll(quote("${GIT_MAVEN_ARGS}"), mavenArgs)
                .replaceAll(quote("${CURRENT_NAMESPACE}"), client.project())
                .replaceAll(quote(QUARKUS_PLATFORM_GROUP_ID_PROPERTY), PLATFORM_GROUP_ID.get())
                .replaceAll(quote(QUARKUS_PLATFORM_VERSION_PROPERTY), quarkusPlatformVersion);
    }

    private String getQuarkusS2iBaseImage() {
        PropertyLookup s2iImageProperty = isNativeTest() ? QUARKUS_NATIVE_S2I_FROM_SRC : QUARKUS_JVM_S2I;
        return model.getContext().getOwner().getProperty(s2iImageProperty.getPropertyKey())
                .orElseGet(() -> s2iImageProperty.get(model.getContext()));
    }

    private void createMavenSettings() {
        Path targetQuarkusSourceS2iSettingsMvnFilename = model.getContext().getServiceFolder()
                .resolve(QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME);
        String content = FileUtils.loadFile("/" + QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME);

        String remoteRepo = MAVEN_REMOTE_REPOSITORY.get(model.getContext());
        if (StringUtils.isNotEmpty(remoteRepo)) {
            content = content.replaceAll(quote(INTERNAL_MAVEN_REPOSITORY_PROPERTY), remoteRepo);
        }

        FileUtils.copyContentTo(content, targetQuarkusSourceS2iSettingsMvnFilename);
        client.apply(targetQuarkusSourceS2iSettingsMvnFilename);
    }

}
