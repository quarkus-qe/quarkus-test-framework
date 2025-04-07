package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContextException;

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

    public QuarkusCliClient.Result update() {
        return update(QuarkusCliClient.UpdateApplicationRequest.defaultUpdate());
    }

    public QuarkusCliClient.Result update(QuarkusCliClient.UpdateApplicationRequest request) {
        return cliClient.updateApplication(request, getServiceFolder());
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
    protected ServiceContext createServiceContext(ScenarioContext scenarioContext) {
        final ServiceContext serviceContext;
        if (serviceFolder != null) {
            serviceContext = new ServiceContext(this, scenarioContext, serviceFolder);
        } else {
            serviceContext = super.createServiceContext(scenarioContext);
        }
        scenarioContext.getTestStore().put(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY, serviceContext);
        return serviceContext;
    }

    @Override
    public void close() {
        var storedContext = context.getScenarioContext().getTestStore().get(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY);
        if (storedContext == context) {
            try {
                context.getScenarioContext().getTestStore().put(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY, null);
            } catch (ExtensionContextException ex) {
                // we can't detect if the store is closed when calling this from NamespacedHierarchicalStore.close
                // details in https://github.com/quarkus-qe/quarkus-test-suite/issues/2376
            }
        }
        super.close();
    }
}
