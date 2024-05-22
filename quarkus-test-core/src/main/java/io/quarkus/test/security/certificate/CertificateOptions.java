package io.quarkus.test.security.certificate;

import java.nio.file.Path;

record CertificateOptions(String prefix, io.quarkus.test.services.Certificate.Format format, String password,
        boolean keystoreProps, boolean truststoreProps, boolean keystoreManagementInterfaceProps,
        io.quarkus.test.services.Certificate.ClientCertificate[] clientCertificates, Path localTargetDir,
        ContainerMountStrategy containerMountStrategy, boolean createPkcs12TsForPem) {
}
