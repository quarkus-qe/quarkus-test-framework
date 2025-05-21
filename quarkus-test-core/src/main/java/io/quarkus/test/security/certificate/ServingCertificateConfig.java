package io.quarkus.test.security.certificate;

import io.quarkus.test.bootstrap.ServiceContext;

public record ServingCertificateConfig(boolean injectCABundle, boolean addServiceCertificate, String tlsConfigName,
        boolean useKeyStoreProvider) {

    public static final String SERVING_CERTIFICATE_KEY = "serving-certificate-config-key";

    public static boolean isServingCertificateScenario(ServiceContext serviceContext) {
        return get(serviceContext) != null;
    }

    public static ServingCertificateConfig get(ServiceContext serviceContext) {
        if (serviceContext.get(SERVING_CERTIFICATE_KEY) instanceof ServingCertificateConfig config) {
            return config;
        }
        return null;
    }

    static ServingCertificateConfigBuilder builder() {
        return new ServingCertificateConfigBuilder();
    }

    static final class ServingCertificateConfigBuilder {

        private boolean injectCABundle = false;
        private boolean addServiceCertificate = false;
        private boolean useKeyStoreProvider = false;
        private String tlsConfigName = null;

        private ServingCertificateConfigBuilder() {
        }

        ServingCertificateConfigBuilder withInjectCABundle(boolean injectCABundle) {
            this.injectCABundle = injectCABundle;
            return this;
        }

        ServingCertificateConfigBuilder withAddServiceCertificate(boolean addServiceCertificate) {
            this.addServiceCertificate = addServiceCertificate;
            return this;
        }

        ServingCertificateConfigBuilder withTlsConfigName(String tlsConfigName) {
            this.tlsConfigName = tlsConfigName;
            return this;
        }

        ServingCertificateConfigBuilder withUseKeyStoreProvider(boolean useKeyStoreProvider) {
            this.useKeyStoreProvider = useKeyStoreProvider;
            return this;
        }

        ServingCertificateConfig build() {
            if (injectCABundle || addServiceCertificate) {
                return new ServingCertificateConfig(injectCABundle, addServiceCertificate, tlsConfigName, useKeyStoreProvider);
            }
            return null;
        }
    }
}
