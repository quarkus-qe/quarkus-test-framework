package io.quarkus.test.bootstrap;

public class QuarkusCliDefaultService extends DefaultService {

    private final QuarkusCliClient cliClient;

    public QuarkusCliDefaultService(QuarkusCliClient cliClient) {
        this.cliClient = cliClient;
    }

    @Override
    public ServiceContext register(String serviceName, ScenarioContext scenarioContext) {
        var serviceContext = super.register(serviceName, scenarioContext);
        scenarioContext.getTestStore().put(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY, serviceContext);
        return serviceContext;
    }

    @Override
    public void close() {
        var storedContext = context.getScenarioContext().getTestStore().get(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY);
        if (storedContext == context) {
            context.getScenarioContext().getTestStore().put(QuarkusCliClient.CLI_SERVICE_CONTEXT_KEY, null);
        }
        super.close();
    }

    public QuarkusCliClient.Result buildOnJvm(String... extraArgs) {
        return cliClient.buildApplicationOnJvm(getServiceFolder(), extraArgs);
    }
}
