package io.quarkus.test.security.certificate;

public final class DelegatingContainerMountStrategy implements ContainerMountStrategy {

    private final ContainerMountStrategy keyStoreStrategy;
    private final ContainerMountStrategy trustStoreStrategy;

    public DelegatingContainerMountStrategy(ContainerMountStrategy keyStoreStrategy,
            ContainerMountStrategy trustStoreStrategy) {
        this.keyStoreStrategy = keyStoreStrategy;
        this.trustStoreStrategy = trustStoreStrategy;
    }

    @Override
    public String truststorePath(String currentLocation) {
        return trustStoreStrategy.truststorePath(currentLocation);
    }

    @Override
    public String keystorePath(String currentLocation) {
        return keyStoreStrategy.keystorePath(currentLocation);
    }

    @Override
    public String keyPath(String currentLocation) {
        return keyStoreStrategy.keyPath(currentLocation);
    }

    @Override
    public String certPath(String currentLocation) {
        return keyStoreStrategy.certPath(currentLocation);
    }

    @Override
    public boolean mountToContainer() {
        return keyStoreStrategy.mountToContainer() || trustStoreStrategy.mountToContainer();
    }

    @Override
    public boolean containerShareMountKeyStorePathWithApp() {
        return keyStoreStrategy.containerShareMountKeyStorePathWithApp();
    }

    @Override
    public boolean containerShareMountTrustStorePathWithApp() {
        return trustStoreStrategy.containerShareMountTrustStorePathWithApp();
    }

    @Override
    public boolean trustStoreRequiresAbsolutePath() {
        return trustStoreStrategy.trustStoreRequiresAbsolutePath();
    }
}
