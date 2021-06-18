package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.builder.Version;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<GitRepositoryQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME = "openjdk-11.yml";
    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "/quarkus-s2i-source-build-template.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";
    private static final String QUARKUS_VERSION_PROPERTY = "${QUARKUS_VERSION}";
    private static final String INTERNAL_MAVEN_REPOSITORY_PROPERTY = "${internal.s2i.maven.remote.repository}";
    private static final String QUARKUS_SNAPSHOT_VERSION = "999-SNAPSHOT";
    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY = new PropertyLookup("s2i.maven.remote.repository");

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    protected String getDefaultTemplate() {
        return QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME;
    }

    /**
     * - Apply base image stream (resources/openjdk-11.yml).
     * - Wait for base image stream (resources/openjdk-11.yml).
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
        waitForBaseImage();
        createMavenSettings();
        super.doInit();
        client.followBuildConfigLogs(model.getContext().getName());
    }

    @Override
    protected boolean needsBuildArtifact() {
        return false;
    }

    @Override
    protected void validate() {
        super.validate();
        if (QUARKUS_SNAPSHOT_VERSION.equals(Version.getVersion())
                && StringUtils.isEmpty(MAVEN_REMOTE_REPOSITORY.get(model.getContext()))) {
            Assertions.fail("s2i can't use the Quarkus 999-SNAPSHOT version if not Maven repository has been provided");
        }
    }

    protected String replaceDeploymentContent(String content) {
        String quarkusVersion = Version.getVersion();
        String mavenArgs = model.getMavenArgs().replaceAll(quote(QUARKUS_VERSION_PROPERTY), quarkusVersion);
        return content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${GIT_URI}"), model.getGitRepository())
                .replaceAll(quote("${GIT_REF}"), model.getGitBranch())
                .replaceAll(quote("${CONTEXT_DIR}"), model.getContextDir())
                .replaceAll(quote("${GIT_MAVEN_ARGS}"), mavenArgs)
                .replaceAll(quote(QUARKUS_VERSION_PROPERTY), quarkusVersion);
    }

    private void waitForBaseImage() {
        Path targetQuarkusSourceS2iBaseImageFileName = model.getContext().getServiceFolder()
                .resolve(QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME);
        FileUtils.copyFileTo(QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME, targetQuarkusSourceS2iBaseImageFileName);
        client.apply(model.getContext().getOwner(), targetQuarkusSourceS2iBaseImageFileName);
        client.awaitFor(model.getContext().getOwner(), targetQuarkusSourceS2iBaseImageFileName);
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
        client.apply(model.getContext().getOwner(), targetQuarkusSourceS2iSettingsMvnFilename);
    }

}
