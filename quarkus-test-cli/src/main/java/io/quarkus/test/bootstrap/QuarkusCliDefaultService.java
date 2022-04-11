package io.quarkus.test.bootstrap;

public class QuarkusCliDefaultService extends DefaultService {

    private final QuarkusCliClient cliClient;

    public QuarkusCliDefaultService(QuarkusCliClient cliClient) {
        this.cliClient = cliClient;
    }

    public QuarkusCliClient.Result buildOnJvm(String... extraArgs) {
        return cliClient.buildApplicationOnJvm(getServiceFolder(), extraArgs);
    }
}
