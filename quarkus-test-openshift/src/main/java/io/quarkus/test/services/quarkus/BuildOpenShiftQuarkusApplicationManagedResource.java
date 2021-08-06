package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.utils.Command;

public class BuildOpenShiftQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<ProdQuarkusApplicationManagedResourceBuilder> {

    private static final String S2I_DEFAULT_VERSION = "latest";

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-build-openshift-template.yml";

    private static final String IMAGE_TAG_SEPARATOR = ":";

    private static final PropertyLookup UBI_QUARKUS_JVM_S2I = new PropertyLookup("quarkus.s2i.base-jvm-image",
            "registry.access.redhat.com/ubi8/openjdk-11:latest");
    private static final PropertyLookup UBI_QUARKUS_NATIVE_S2I = new PropertyLookup("quarkus.s2i.base-native-image",
            "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0");

    public BuildOpenShiftQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
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
    }

    private void startBuild() {
        String fromArg = "--from-dir=" + model.getArtifact().toAbsolutePath().getParent().toString();
        if (isNativeTest()) {
            fromArg = "--from-file=" + model.getArtifact().toAbsolutePath().toString();
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
        PropertyLookup s2iImageProperty = UBI_QUARKUS_JVM_S2I;
        if (isNativeTest()) {
            s2iImageProperty = UBI_QUARKUS_NATIVE_S2I;
        }

        return s2iImageProperty.get(model.getContext());
    }
}
