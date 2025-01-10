package io.quarkus.test.bootstrap;

public final class QuarkusVersionAwareCliClient extends QuarkusCliClient {

    public enum SetCliPlatformVersionMode {
        SNAPSHOT(""),
        FIXED_VERSION(" set explicitly"),
        NO_VERSION("latest released Quarkus");

        private final String quarkusVersionPostfix;

        SetCliPlatformVersionMode(String quarkusVersionPostfix) {
            this.quarkusVersionPostfix = quarkusVersionPostfix;
        }

        public String getQuarkusVersionPostfix() {
            return quarkusVersionPostfix;
        }
    }

    private final SetCliPlatformVersionMode mode;
    private final String quarkusVersion;

    public QuarkusVersionAwareCliClient(QuarkusCliClient cliClient, SetCliPlatformVersionMode mode, String quarkusVersion) {
        super(cliClient.getScenarioContext());
        this.mode = mode;
        this.quarkusVersion = quarkusVersion;
    }

    @Override
    public CreateApplicationRequest getDefaultCreateApplicationRequest() {
        return switch (mode) {
            case SNAPSHOT -> new CreateApplicationRequest().withCurrentPlatformBom();
            case FIXED_VERSION -> new CreateApplicationRequest().withStream(getFixedStreamVersion());
            // it is completely safe to not set platform
            // because 'NO_VERSION' is only tested with the snapshots
            // and RHBQ can never be snapshot, so we are testing the latest community version
            // expected behavior for this option: you create app with CLI v 3.14 and the latest released is 3.17.6
            // result: Quarkus app with 3.17.6 is created
            case NO_VERSION -> new CreateApplicationRequest();
        };
    }

    @Override
    public CreateExtensionRequest getDefaultCreateExtensionRequest() {
        return switch (mode) {
            case SNAPSHOT -> new CreateExtensionRequest().withCurrentPlatformBom();
            case FIXED_VERSION -> new CreateExtensionRequest().withStream(getFixedStreamVersion());
            case NO_VERSION -> new CreateExtensionRequest();
        };
    }

    @Override
    public String getQuarkusVersion() {
        return quarkusVersion;
    }
}
