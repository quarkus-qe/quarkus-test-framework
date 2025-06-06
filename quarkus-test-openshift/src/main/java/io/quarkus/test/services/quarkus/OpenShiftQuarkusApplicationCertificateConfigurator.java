package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.security.certificate.CertificateBuilder;

public abstract class OpenShiftQuarkusApplicationCertificateConfigurator {
    public static final String KEYSTORE_MOUNT_PATH = "/mnt/keystore/";
    public static final String TRUSTSTORE_MOUNT_PATH = "/mnt/truststore/";

    public static final String PROPERTY_KEYSTORE_SECRET_NAME = "keystoreSecretName";
    public static final String PROPERTY_TRUSTSTORE_SECRET_NAME = "truststoreSecretName";

    private static final String KEYSTORE_SECRET_SUFFIX = "-keystore";
    private static final String TRUSTSTORE_SECRET_SUFFIX = "-truststore";

    public static void configureCertificates(CertificateBuilder certificateBuilder, ServiceContext context) {
        OpenShiftClient client = context.get(OpenShiftExtensionBootstrap.CLIENT);

        String appName = context.getName();

        if (certificateBuilder != null && !certificateBuilder.certificates().isEmpty()) {
            if (certificateBuilder.certificates().size() > 1) {
                fail("Apps on OpensShift currently support maximum of 1 certificate");
            }
            context.put(CertificateBuilder.INSTANCE_KEY, certificateBuilder);

            // create secrets on OCP
            Certificate certificate = certificateBuilder.certificates().get(0);
            client.doCreateSecretFromFile(appName + KEYSTORE_SECRET_SUFFIX, certificate.keystorePath());
            client.doCreateSecretFromFile(appName + TRUSTSTORE_SECRET_SUFFIX, certificate.truststorePath());

            String keystoreFilename = FilenameUtils.getName(certificate.keystorePath());
            String truststoreFilename = FilenameUtils.getName(certificate.truststorePath());

            // override keystore and truststore location as they will be mounted elsewhere on OCP
            Map<String, String> configProperties = new HashMap<>(certificate.configProperties());
            configProperties.put("quarkus.tls.tls-server.key-store.p12.path", KEYSTORE_MOUNT_PATH + keystoreFilename);
            configProperties.put("quarkus.tls.tls-server.trust-store.p12.path", TRUSTSTORE_MOUNT_PATH + truststoreFilename);

            configProperties.forEach(context::withTestScopeConfigProperty);

            // store secret names for future mounting
            context.put(PROPERTY_KEYSTORE_SECRET_NAME, appName + KEYSTORE_SECRET_SUFFIX);
            context.put(PROPERTY_TRUSTSTORE_SECRET_NAME, appName + TRUSTSTORE_SECRET_SUFFIX);
        }
    }
}
