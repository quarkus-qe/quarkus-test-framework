package io.quarkus.test.security.certificate;

import java.nio.file.Path;

public record CertificateOptions(String prefix, io.quarkus.test.services.Certificate.Format format, String password,
        boolean keystoreProps, boolean truststoreProps, boolean configureManagementInterface,
        ClientCertificateRequest[] clientCertificates, Path localTargetDir,
        ContainerMountStrategy containerMountStrategy, boolean createPkcs12TsForPem,
        String serverTrustStoreLocation, String serverKeyStoreLocation, String keyLocation, String certLocation,
        boolean tlsRegistryEnabled, String tlsConfigName, boolean configureHttpServer) {

}
