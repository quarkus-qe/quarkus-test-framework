package io.quarkus.test.security.certificate;

public record FixedPathContainerMountStrategy(String truststorePath, String keystorePath, String keyPath,
        String certPath) implements ContainerMountStrategy {

    @Override
    public String truststorePath(String currentLocation) {
        return truststorePath();
    }

    @Override
    public String keystorePath(String currentLocation) {
        return keystorePath();
    }

    @Override
    public String keyPath(String currentLocation) {
        return keyPath();
    }

    @Override
    public String certPath(String currentLocation) {
        return certPath();
    }

    @Override
    public boolean mountToContainer() {
        return true;
    }

}
