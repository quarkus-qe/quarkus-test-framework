package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.codehaus.plexus.util.StringUtils;

import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.utils.FileUtils;

public class QuarkusSourceS2iBuildApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME = "openjdk-11.yml";
    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "quarkus-s2i-source-build-template.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";

    public QuarkusSourceS2iBuildApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
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

    @Override
    public void validate() {
        super.validate();

        if (model.isSelectedAppClasses()) {
            fail("Custom source classes as @QuarkusApplication(classes = ...) is not supported by Quarkus source S2I build.");
        }

        if (StringUtils.isEmpty(model.getGitRepositoryUri())) {
            fail("Source S2I build requires a remote Git repository with a Quarkus application.");
        }

        if (model.getLaunchMode().equals(LaunchMode.NATIVE)) {
            fail("Quarkus source S2I build is not supported with native launch mode.");
        }
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
        FileUtils.copyFileTo(QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME, targetQuarkusSourceS2iSettingsMvnFilename);
        client.apply(model.getContext().getOwner(), targetQuarkusSourceS2iSettingsMvnFilename);
    }

    private void applyTemplate() {
        Path targetQuarkusSourceS2iBuildTemplateFileName = model.getContext().getServiceFolder()
                .resolve(QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME);
        FileUtils.copyFileTo(QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME,
                targetQuarkusSourceS2iBuildTemplateFileName);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(),
                "/" + QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME,
                this::replaceDeploymentContent,
                targetQuarkusSourceS2iBuildTemplateFileName);
    }

    private String replaceDeploymentContent(String content) {
        return content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${GIT_URI}"), model.getGitRepositoryUri())
                .replaceAll(quote("${GIT_REF}"), model.getGitRef())
                .replaceAll(quote("${CONTEXT_DIR}"), model.getContextDir())
                .replaceAll(quote("${QUARKUS_VERSION}"), model.getQuarkusBuildVersion());
    }

}
