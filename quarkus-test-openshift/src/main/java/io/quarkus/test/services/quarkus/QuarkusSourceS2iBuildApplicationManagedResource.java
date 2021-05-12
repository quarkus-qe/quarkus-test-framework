package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.codehaus.plexus.util.StringUtils;

import io.quarkus.test.utils.FileUtils;

public class QuarkusSourceS2iBuildApplicationManagedResource extends OpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME = "openjdk-11.yml";
    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "quarkus-s2i-source-build-template.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final int INTERNAL_PORT_DEFAULT = 8080;

    private Path tmpQuarkusSourceS2iBaseImageFileName;
    private Path tmpQuarkusSourceS2iBuildTemplateFileName;
    private Path tmpQuarkusSourceS2iSettingsMvnFilename;

    public QuarkusSourceS2iBuildApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
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
        waitForQuarkusApplication();
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

        if (StringUtils.isEmpty(model.getSourceS2iRepositoryUri())) {
            fail("Source S2I build requires a remote Git repository with a Quarkus application!");
        }
    }

    private void waitForBaseImage() {
        tmpQuarkusSourceS2iBaseImageFileName = FileUtils.copyResourceIntoTempFile(QUARKUS_SOURCE_S2I_BASE_IMAGE_FILENAME);
        client.apply(model.getContext().getOwner(), tmpQuarkusSourceS2iBaseImageFileName);
        client.awaitFor(model.getContext().getOwner(), tmpQuarkusSourceS2iBaseImageFileName);
    }

    private void createMavenSettings() {
        tmpQuarkusSourceS2iSettingsMvnFilename = FileUtils.copyResourceIntoTempFile(QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME);
        client.apply(model.getContext().getOwner(), tmpQuarkusSourceS2iSettingsMvnFilename);
    }

    private void waitForQuarkusApplication() {
        client.awaitFor(model.getContext().getOwner(), tmpQuarkusSourceS2iBuildTemplateFileName);
    }

    private void applyTemplate() {
        tmpQuarkusSourceS2iBuildTemplateFileName = FileUtils
                .copyResourceIntoTempFile(QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME);
        client.applyServicePropertiesUsingTemplate(model.getContext().getOwner(),
                "/" + QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME,
                this::replaceDeploymentContent,
                tmpQuarkusSourceS2iBuildTemplateFileName);
    }

    private String replaceDeploymentContent(String content) {
        String result = content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${GIT_URI}"), model.getSourceS2iRepositoryUri());
        if (StringUtils.isNotEmpty(model.getSourceS2iContextDir())) {
            result = client.addContextDirToBuildConfig(model.getContext().getOwner().getName(), model.getSourceS2iContextDir(),
                    result);
        }
        if (StringUtils.isNotEmpty(model.getSourceS2iGitRef())) {
            result = client.addGitRefToBuildConfig(model.getContext().getOwner().getName(), model.getSourceS2iGitRef(), result);
        }
        if (model.getSourceS2iEnvVars().length > 0) {
            result = client.addEnvVarsToDeploymentConfig(model.getContext().getOwner().getName(), model.getSourceS2iEnvVars(),
                    result);
        }
        return result;
    }

}
