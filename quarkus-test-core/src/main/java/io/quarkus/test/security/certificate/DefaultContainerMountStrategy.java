package io.quarkus.test.security.certificate;

import static io.quarkus.test.security.certificate.Certificate.createCertsTempDir;
import static io.quarkus.test.utils.TestExecutionProperties.isKubernetesPlatform;
import static io.quarkus.test.utils.TestExecutionProperties.isOpenshiftPlatform;

import java.nio.file.Path;

import io.quarkus.test.utils.FileUtils;

class DefaultContainerMountStrategy implements ContainerMountStrategy {

    private final String prefix;

    DefaultContainerMountStrategy(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String truststorePath(String currentLocation) {
        // no point of making both keystore and truststore unique, one of them is enough
        return currentLocation;
    }

    @Override
    public String keystorePath(String currentLocation) {
        return makeFileMountPathUnique(prefix, currentLocation);
    }

    @Override
    public String keyPath(String currentLocation) {
        return makeFileMountPathUnique(prefix, currentLocation);
    }

    @Override
    public String certPath(String currentLocation) {
        return makeFileMountPathUnique(prefix, currentLocation);
    }

    @Override
    public boolean containerShareMountPathWithApp() {
        return true;
    }

    @Override
    public boolean mountToContainer() {
        return isOpenshiftPlatform() || isKubernetesPlatform();
    }

    private static String makeFileMountPathUnique(String prefix, String storeLocation) {
        var newTempCertDir = createCertsTempDir(prefix);
        var storeFile = Path.of(storeLocation).toFile();
        FileUtils.copyFileTo(storeFile, newTempCertDir);
        return newTempCertDir.resolve(storeFile.getName()).toAbsolutePath().toString();
    }
}
