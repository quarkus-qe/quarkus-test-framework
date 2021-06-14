package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.nio.file.Path;

import io.quarkus.builder.Version;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME = "openjdk-11.yml";
    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "quarkus-s2i-source-build-template.yml";
    private static final String DEPLOYMENT = "openshift.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";
    private static final String QUARKUS_VERSION_PROPERTY = "${QUARKUS_VERSION}";
    private static final String INTERNAL_MAVEN_REPOSITORY_PROPERTY = "${internal.s2i.maven.remote.repository}";
    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY = new PropertyLookup("s2i.maven.remote.repository", EMPTY);

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
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
        applyTemplate();
        client.followBuildConfigLogs(model.getContext().getName());
    }

    @Override
    protected void doUpdate() {
        applyTemplate();
    }

    @Override
    protected boolean needsBuildArtifact() {
        return false;
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
        String content = FileUtils.loadFile("/" + QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME)
                .replaceAll(quote(INTERNAL_MAVEN_REPOSITORY_PROPERTY), MAVEN_REMOTE_REPOSITORY.get(model.getContext()));
        FileUtils.copyContentTo(content, targetQuarkusSourceS2iSettingsMvnFilename);
        client.apply(model.getContext().getOwner(), targetQuarkusSourceS2iSettingsMvnFilename);
    }

    private void applyTemplate() {
        Path targetQuarkusSourceS2iBuildTemplateFileName = model.getContext().getServiceFolder().resolve(DEPLOYMENT);
        FileUtils.copyFileTo(QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME,
                targetQuarkusSourceS2iBuildTemplateFileName);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(),
                "/" + QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME,
                this::replaceDeploymentContent,
                targetQuarkusSourceS2iBuildTemplateFileName);
    }

    private String replaceDeploymentContent(String content) {
        String quarkusVersion = Version.getVersion();
        String mavenArgs = model.getMavenArgs().replaceAll(quote(QUARKUS_VERSION_PROPERTY), quarkusVersion);
        return content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${GIT_URI}"), model.getGitRepository())
                .replaceAll(quote("${GIT_REF}"), model.getGitBranch())
                .replaceAll(quote("${CONTEXT_DIR}"), model.getContextDir())
                .replaceAll(quote("${GIT_MAVEN_ARGS}"), mavenArgs)
                .replaceAll(quote(QUARKUS_VERSION_PROPERTY), quarkusVersion);
    }

}
