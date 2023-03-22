package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_NATIVE_S2I;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.Command;

public class BuildOpenShiftQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<ArtifactQuarkusApplicationManagedResourceBuilder> {

    private static final String S2I_DEFAULT_VERSION = "latest";

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-build-openshift-template.yml";
    private static final String IMAGE_TAG_SEPARATOR = ":";

    public BuildOpenShiftQuarkusApplicationManagedResource(ArtifactQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected String getDefaultTemplate() {
        return QUARKUS_OPENSHIFT_TEMPLATE;
    }

    @Override
    protected void doInit() {
        super.doInit();
        startBuild();
        exposeServices();
    }

    protected String replaceDeploymentContent(String content) {
        String s2iImage = getS2iImage();
        String s2iVersion = getS2iImageVersion(s2iImage);

        return content.replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER}"),
                StringUtils.substringBeforeLast(s2iImage, IMAGE_TAG_SEPARATOR))
                .replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER_VERSION}"), s2iVersion)
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString());
    }

    private void exposeServices() {
        client.expose(model.getContext().getOwner(), HTTP_PORT_DEFAULT);
        client.expose(model.getContext().getOwner().getName() + "-management", model.getManagementPort());
    }

    private void startBuild() {
        String fromArg = "--from-dir=" + model.getArtifact().toAbsolutePath().getParent().toString();
        if (isNativeTest()) {
            fromArg = "--from-file=" + model.getArtifact().toAbsolutePath();
        }

        try {
            new Command("oc", "start-build", model.getContext().getName(), fromArg, "--follow").runAndWait();
        } catch (Exception e) {
            fail("Failed when starting build. Caused by " + e.getMessage());
        }
    }

    private String getS2iImageVersion(String s2iImage) {
        String s2iVersion = S2I_DEFAULT_VERSION;
        if (s2iImage.contains(IMAGE_TAG_SEPARATOR)) {
            s2iVersion = StringUtils.substringAfterLast(s2iImage, IMAGE_TAG_SEPARATOR);
        }

        return s2iVersion;
    }

    private String getS2iImage() {
        PropertyLookup s2iImageProperty = isNativeTest() ? QUARKUS_NATIVE_S2I : QUARKUS_JVM_S2I;
        return model.getContext().getOwner().getProperty(s2iImageProperty.getPropertyKey())
                .orElseGet(() -> s2iImageProperty.get(model.getContext()));
    }
}
