package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder.HTTP_PORT_DEFAULT;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_NATIVE_S2I;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.PropertiesUtils;

public class BuildOpenShiftQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<ArtifactQuarkusApplicationManagedResourceBuilder> {

    private static final String S2I_DEFAULT_VERSION = "latest";

    private static final String QUARKUS_OPENSHIFT_TEMPLATE = "/quarkus-build-openshift-template.yml";
    private static final String IMAGE_TAG_SEPARATOR = ":";
    private static final String QUARKUS_OPENSHIFT_OPTS_PROPERTY = "quarkus.openshift.env.vars.quarkus-opts";

    private String builtImageName = "no-image-yet";

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
        this.builtImageName = getImage(model.getContext().getName());
        patchDeployment(model.getContext().getName(), this.builtImageName);
        exposeServices();
    }

    /*
     * Template is created and deployed before the build,
     * so we do not know image name when we do `oc apply`
     * so we need to change the deployment on cluster before the first start
     */
    private static void patchDeployment(String service, String image) {
        Log.info("Trying to patch deployment with the latest image version.");
        try {
            HashMap<Object, Object> replacement = new HashMap<>();
            replacement.put("op", "replace");
            replacement.put("path", "/spec/template/spec/containers/0/image");
            replacement.put("value", image);
            String patch = new ObjectMapper().writer().writeValueAsString(replacement);
            new Command("oc", "patch", "deployment", service, "--type=json",
                    "-p=[%s]".formatted(patch))
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed while patching the deployment. Caused by " + e.getMessage());
        }
    }

    private static String getImage(String service) {
        Log.info("Trying to retrieve the latest image version.");
        try {
            List<String> output = new ArrayList<>();
            new Command("oc", "get", "buildconfig", service, "-o", "template",
                    "--template",
                    "{{.status.lastVersion}}")
                    .outputToLines(output)
                    .runAndWait();
            String version = output.get(0);
            Log.info("Received build version %s", version);
            output = new ArrayList<>();
            new Command("oc", "get", "build", service + "-" + version,
                    "-o", "template",
                    "--template",
                    "{{.status.outputDockerImageReference}}")
                    .outputToLines(output)
                    .runAndWait();
            String image = output.get(0);
            Log.info("Received full image name: '%s'", image);
            return image;
        } catch (Exception e) {
            throw new AssertionFailedError("Failed while retrieving image. Caused by " + e.getMessage());
        }
    }

    protected String replaceDeploymentContent(String content) {
        String s2iImage = getS2iImage();
        String s2iVersion = getS2iImageVersion(s2iImage);
        String quarkusOpts = getQuarkusOpts();

        return content.replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER}"),
                StringUtils.substringBeforeLast(s2iImage, IMAGE_TAG_SEPARATOR))
                .replaceAll(quote("${QUARKUS_S2I_IMAGE_BUILDER_VERSION}"), s2iVersion)
                .replaceAll(quote("${ARTIFACT}"), model.getArtifact().getFileName().toString())
                .replaceAll(quote("${QUARKUS_OPTS}"), quarkusOpts)
                .replaceAll(quote("${IMAGE_NAME}"), this.builtImageName);
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

    private String getQuarkusOpts() {
        Path applicationProperties = model.getComputedApplicationProperties();
        if (Files.exists(applicationProperties)) {
            Map<String, String> properties = PropertiesUtils.toMap(applicationProperties);
            if (properties.containsKey(QUARKUS_OPENSHIFT_OPTS_PROPERTY)) {
                return properties.get(QUARKUS_OPENSHIFT_OPTS_PROPERTY);
            }
        }
        return "";
    }
}
