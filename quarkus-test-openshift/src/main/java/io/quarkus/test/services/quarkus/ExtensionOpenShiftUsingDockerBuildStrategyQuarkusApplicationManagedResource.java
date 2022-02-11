package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.MavenUtils.withProperty;
import static java.util.regex.Pattern.quote;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.test.utils.DockerUtils;
import io.quarkus.test.utils.FileUtils;

public class ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource
        extends ExtensionOpenShiftQuarkusApplicationManagedResource {

    private static final String QUARKUS_OPENSHIFT_BUILD_STRATEGY = "quarkus.openshift.build-strategy";
    private static final String DOCKER = "docker";

    private static final String DOCKERFILE_SOURCE_FOLDER = "src/main/docker";

    public ExtensionOpenShiftUsingDockerBuildStrategyQuarkusApplicationManagedResource(
            ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected void withAdditionalArguments(List<String> args) {
        copyDockerfileToSources();

        args.add(withProperty(QUARKUS_OPENSHIFT_BUILD_STRATEGY, DOCKER));
    }

    private void copyDockerfileToSources() {
        String dockerfileName = DockerUtils.getDockerfile(getLaunchMode());
        Path dockerfileTargetFile = model.getApplicationFolder().resolve(DOCKERFILE_SOURCE_FOLDER + dockerfileName);
        Path dockerfileTargetFolder = dockerfileTargetFile.getParent();
        if (!Files.exists(dockerfileTargetFolder)) {
            FileUtils.createDirectory(dockerfileTargetFolder);
        }

        if (!Files.exists(dockerfileTargetFile)) {
            String dockerFileContent = FileUtils.loadFile(DockerUtils.getDockerfile(getLaunchMode()))
                    .replaceAll(quote("${ARTIFACT_PARENT}"), "target");
            FileUtils.copyContentTo(dockerFileContent, dockerfileTargetFile);
        }
    }
}
