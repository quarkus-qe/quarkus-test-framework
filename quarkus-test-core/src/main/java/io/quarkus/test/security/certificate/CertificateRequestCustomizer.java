package io.quarkus.test.security.certificate;

import java.nio.file.Path;
import java.util.List;

public interface CertificateRequestCustomizer {

    void customize(CertificateRequest request);

    interface CertificateRequest {
        CertificateRequest withPassword(String password);

        CertificateRequest withClientRequests(ClientCertificateRequest... clientRequests);

        CertificateRequest withServerTrustStoreLocation(String serverTrustStoreLocation);

        CertificateRequest withServerKeyStoreLocation(String serverKeyStoreLocation);

        CertificateRequest withPemKeyLocation(String keyLocation);

        CertificateRequest withPemCertLocation(String certLocation);

        CertificateRequest withBaseDirForAllCerts(Path localTargetDir);

        CertificateRequest withSubjectAlternativeNames(List<String> subjectAlternativeNames);
    }

    final class CertificateRequestImpl implements CertificateRequest {

        private CertificateOptions certificateOptions;
        private String password;
        private ClientCertificateRequest[] clientRequests;
        private Path localTargetDir;
        private String serverTrustStoreLocation = null;
        private String serverKeyStoreLocation = null;
        private String keyLocation = null;
        private String certLocation = null;
        private List<String> subjectAlternativeNames = null;

        CertificateRequestImpl(CertificateOptions originalOpts) {
            this.certificateOptions = originalOpts;
            this.password = originalOpts.password();
            this.clientRequests = originalOpts.clientCertificates();
            this.localTargetDir = originalOpts.localTargetDir();
        }

        CertificateOptions createNewOptions() {
            certificateOptions = new CertificateOptions(certificateOptions.prefix(), certificateOptions.format(), password,
                    certificateOptions.keystoreProps(), certificateOptions.truststoreProps(),
                    certificateOptions.configureManagementInterface(), clientRequests, localTargetDir,
                    certificateOptions.containerMountStrategy(), certificateOptions.createPkcs12TsForPem(),
                    serverTrustStoreLocation, serverKeyStoreLocation, keyLocation, certLocation,
                    certificateOptions.tlsRegistryEnabled(), certificateOptions.tlsConfigName(),
                    certificateOptions.configureHttpServer(), subjectAlternativeNames);
            return certificateOptions;
        }

        @Override
        public CertificateRequest withPassword(String password) {
            this.password = password;
            return this;
        }

        @Override
        public CertificateRequest withClientRequests(ClientCertificateRequest... clientRequests) {
            this.clientRequests = clientRequests;
            return this;
        }

        @Override
        public CertificateRequest withServerTrustStoreLocation(String serverTrustStoreLocation) {
            this.serverTrustStoreLocation = serverTrustStoreLocation;
            return this;
        }

        @Override
        public CertificateRequest withServerKeyStoreLocation(String serverKeyStoreLocation) {
            this.serverKeyStoreLocation = serverKeyStoreLocation;
            return this;
        }

        @Override
        public CertificateRequest withPemKeyLocation(String keyLocation) {
            this.keyLocation = keyLocation;
            return this;
        }

        @Override
        public CertificateRequest withPemCertLocation(String certLocation) {
            this.certLocation = certLocation;
            return this;
        }

        @Override
        public CertificateRequest withBaseDirForAllCerts(Path localTargetDir) {
            this.localTargetDir = localTargetDir;
            return this;
        }

        @Override
        public CertificateRequest withSubjectAlternativeNames(List<String> subjectAlternativeNames) {
            this.subjectAlternativeNames = subjectAlternativeNames;
            return this;
        }

        String getCertLocation() {
            return certLocation;
        }

        String getKeyLocation() {
            return keyLocation;
        }

        String getServerKeyStoreLocation() {
            return serverKeyStoreLocation;
        }

        String getServerTrustStoreLocation() {
            return serverTrustStoreLocation;
        }
    }
}
