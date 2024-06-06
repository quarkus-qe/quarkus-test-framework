package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QuarkusCliRestService extends RestService {

    private final QuarkusCliClient cliClient;
    private final Path serviceFolder;

    public QuarkusCliRestService(QuarkusCliClient cliClient, Path serviceFolder) {
        this.cliClient = cliClient;
        this.serviceFolder = serviceFolder;
    }

    public QuarkusCliClient.Result buildOnJvm(String... extraArgs) {
        return cliClient.buildApplicationOnJvm(getServiceFolder(), extraArgs);
    }

    public QuarkusCliClient.Result buildOnNative(String... extraArgs) {
        return cliClient.buildApplicationOnNative(getServiceFolder(), extraArgs);
    }

    public QuarkusCliClient.Result installExtension(String extension) {
        return cliClient.run(getServiceFolder(), "extension", "add", extension);
    }

    public QuarkusCliClient.Result removeExtension(String extension) {
        return cliClient.run(getServiceFolder(), "extension", "remove", extension);
    }

    public List<String> getInstalledExtensions() {
        QuarkusCliClient.Result result = cliClient.run(getServiceFolder(), "extension", "list", "--id");
        assertTrue(result.isSuccessful(), "Extension list failed");
        return result.getOutput().lines().map(String::trim)
                .map(line -> line.replace("✬ ", "")).collect(Collectors.toList());
    }

    public File getFileFromApplication(String fileName) {
        // get file from the service folder
        return getFileFromApplication("", fileName);
    }

    public File getFileFromApplication(String subFolder, String fileName) {
        Path fileFolderPath = getServiceFolder();
        if (subFolder != null && !subFolder.isEmpty()) {
            fileFolderPath = Path.of(fileFolderPath.toString(), subFolder);
        }

        return Arrays.stream(Objects.requireNonNull(fileFolderPath.toFile().listFiles()))
                .filter(f -> f.getName().equalsIgnoreCase(fileName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(fileName + " not found."));
    }

    @Override
    protected ServiceContext createServiceContext(ScenarioContext context) {
        if (serviceFolder != null) {
            return new ServiceContext(this, context, serviceFolder);
        }
        return super.createServiceContext(context);
    }
}
