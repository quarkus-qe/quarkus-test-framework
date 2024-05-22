package io.quarkus.test.security.certificate;

public interface ContainerMountStrategy {

    String truststorePath(String currentLocation);

    String keystorePath(String currentLocation);

    String keyPath(String currentLocation);

    String certPath(String currentLocation);

    /**
     * Whether container destination path is also path used by Quarkus application when accessing these certs.
     * Simply put it, if 'yes' is returned, we are probably mounting certs to the Quarkus application pod.
     */
    boolean containerShareMountPathWithApp();

    /**
     * Whether certificates should be mounted to the container.
     */
    boolean mountToContainer();
}
