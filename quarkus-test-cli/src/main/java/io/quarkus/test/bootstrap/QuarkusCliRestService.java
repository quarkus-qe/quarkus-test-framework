package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

public class QuarkusCliRestService extends RestService {

    private final QuarkusCliClient cliClient;

    public QuarkusCliRestService(QuarkusCliClient cliClient) {
        this.cliClient = cliClient;
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
        QuarkusCliClient.Result result = cliClient.run(getServiceFolder(), "extension", "list");
        assertTrue(result.isSuccessful(), "Extension list failed");
        return result.getOutput().lines().map(String::trim).collect(Collectors.toList());
    }

}
