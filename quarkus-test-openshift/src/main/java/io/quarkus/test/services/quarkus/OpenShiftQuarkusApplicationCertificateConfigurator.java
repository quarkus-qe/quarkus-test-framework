package io.quarkus.test.services.quarkus;

import static io.quarkus.test.bootstrap.inject.OpenShiftClient.TLS_ROUTE_SUFFIX;
import static io.quarkus.test.security.certificate.ServingCertificateConfig.SERVING_CERTIFICATE_KEY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (certificateBuilder == null) {
            return;
        }

        if (!certificateBuilder.certificates().isEmpty()) {
            OpenShiftClient client = context.get(OpenShiftExtensionBootstrap.CLIENT);
            String appName = context.getName();

            context.put(CertificateBuilder.INSTANCE_KEY, certificateBuilder);

            // inject OCP SANs into the certificates
            certificateBuilder.certificates().forEach(
                    certificate -> certificateBuilder.regenerateCertificate(certificate.prefix(),
                            certReq -> certReq.withSubjectAlternativeNames(generateSubjectAlternateNames(client, appName))));

            // create secrets on OCP
            // override keystore and truststore location as they will be mounted elsewhere on OCP
            // taking paths from first certificate. Usually there is only one certificate,
            // and if there are more, mounting should be handled otherwise anyway.
            Certificate certificate = certificateBuilder.certificates().get(0);
            Map<String, String> configProperties = new HashMap<>(certificate.configProperties());

            if (certificate.keystorePath() != null) {
                client.doCreateSecretFromFile(appName + KEYSTORE_SECRET_SUFFIX, certificate.keystorePath());
                String keystoreFilename = FilenameUtils.getName(certificate.keystorePath());
                overrideKeystorePaths(configProperties, KEYSTORE_MOUNT_PATH + keystoreFilename);
                // store secret names for future mounting
                context.put(PROPERTY_KEYSTORE_SECRET_NAME, appName + KEYSTORE_SECRET_SUFFIX);
            }
            if (certificate.truststorePath() != null) {
                client.doCreateSecretFromFile(appName + TRUSTSTORE_SECRET_SUFFIX, certificate.truststorePath());
                String truststoreFilename = FilenameUtils.getName(certificate.truststorePath());
                overrideTruststorePaths(configProperties, TRUSTSTORE_MOUNT_PATH + truststoreFilename);
                // store secret names for future mounting
                context.put(PROPERTY_TRUSTSTORE_SECRET_NAME, appName + TRUSTSTORE_SECRET_SUFFIX);
            }

            configProperties.forEach(context::withTestScopeConfigProperty);
        }
        if (certificateBuilder.servingCertificateConfig() != null) {
            context.put(SERVING_CERTIFICATE_KEY, certificateBuilder.servingCertificateConfig());
        }
    }

    /**
     * Override keystore and truststore path to ones used in OCP.
     */
    private static void overrideKeystorePaths(Map<String, String> configProperties, String keystorePath) {
        // it is not necessary to have keystore and/or truststore configured
        // but there can also be multiple properties using keystore or truststore (like HTTP and Management ports)
        // replace paths only for those, that are configured
        Set<String> keystorePropertyNames = configProperties.keySet().stream()
                .filter(string -> string.contains("key-store") && (string.endsWith("path") || string.endsWith("file")))
                .collect(Collectors.toSet());
        keystorePropertyNames.forEach(s -> configProperties.put(s, keystorePath));
    }

    private static void overrideTruststorePaths(Map<String, String> configProperties, String truststorePath) {
        // it is not necessary to have keystore and/or truststore configured
        // but there can also be multiple properties using keystore or truststore (like HTTP and Management ports)
        // replace paths only for those, that are configured
        Set<String> truststorePropertyNames = configProperties.keySet().stream()
                .filter(string -> string.contains("trust-store") && (string.endsWith("path") || string.endsWith("file")))
                .collect(Collectors.toSet());
        truststorePropertyNames.forEach(s -> configProperties.put(s, truststorePath));
    }

    /**
     * Generate SubjectAlternateNames (SANs) for OCP service and route, which the service will be accessible via.
     * These should be added into the certificate.
     */
    private static List<String> generateSubjectAlternateNames(OpenShiftClient client, String appName) {
        return Arrays.asList(
                // add SAN for service
                appName + TLS_ROUTE_SUFFIX,
                // add SAN for route
                client.predictRouteHost(appName + TLS_ROUTE_SUFFIX));
    }
}
