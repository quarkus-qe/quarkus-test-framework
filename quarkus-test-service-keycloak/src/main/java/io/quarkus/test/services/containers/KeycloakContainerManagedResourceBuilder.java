package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.KeycloakService.KEYSTORE_PASSWORD;
import static io.quarkus.test.utils.TestExecutionProperties.isKubernetesPlatform;
import static io.quarkus.test.utils.TestExecutionProperties.isOpenshiftPlatform;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.security.certificate.ContainerMountStrategy;
import io.quarkus.test.security.certificate.DelegatingContainerMountStrategy;
import io.quarkus.test.security.certificate.FixedPathContainerMountStrategy;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.utils.PropertiesUtils;

public class KeycloakContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    public static final String CERTIFICATE_CONTEXT_KEY = "io.quarkus.test.services.containers.keycloak.certificate";
    public static final String KEYCLOAK_PRODUCTION_MODE_KEY = "io.quarkus.test.services.keycloak.production.mode";
    public static final String KEYCLOAK = "keycloak";
    private static final String MOUNTED_KEYSTORE_NAME = KEYCLOAK + "-keystore";
    private static final String KEYSTORE_DEST_PATH = "/opt/keycloak/conf/";
    private static final String MOUNTED_KEYSTORE_PATH = KEYSTORE_DEST_PATH + MOUNTED_KEYSTORE_NAME;
    private static final String MOUNTED_KEY_PATH = KEYSTORE_DEST_PATH + "key/" + KEYCLOAK + ".key";
    private static final String MOUNTED_CERT_PATH = KEYSTORE_DEST_PATH + "cert/" + KEYCLOAK + ".crt";

    private final ServiceLoader<KeycloakContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(KeycloakContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private int restPort;
    private int tlsRestPort;
    private String[] command;
    private String expectedLog;
    private long memoryLimitMiB;
    private boolean runKeycloakInProdMode;
    private boolean sslEnabled;
    private Certificate.Format certificateFormat;

    @Override
    protected String getImage() {
        return image;
    }

    @Override
    protected Integer getPort() {
        return restPort;
    }

    @Override
    protected Integer getTlsPort() {
        return tlsRestPort;
    }

    @Override
    protected boolean isSslEnabled() {
        return sslEnabled;
    }

    @Override
    protected String[] getCommand() {
        return command;
    }

    @Override
    protected ServiceContext getContext() {
        return context;
    }

    @Override
    protected String getExpectedLog() {
        return expectedLog;
    }

    protected Long getMemoryLimitMiB() {
        return memoryLimitMiB;
    }

    protected boolean runKeycloakInProdMode() {
        return runKeycloakInProdMode;
    }

    protected Certificate.Format certificateFormat() {
        return certificateFormat;
    }

    @Override
    public void init(Annotation annotation) {
        KeycloakContainer metadata = (KeycloakContainer) annotation;
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.restPort = metadata.port();
        this.tlsRestPort = metadata.tlsPort();
        this.expectedLog = PropertiesUtils.resolveProperty(metadata.expectedLog());
        this.command = metadata.command();
        this.memoryLimitMiB = metadata.memoryLimitMiB();
        this.runKeycloakInProdMode = metadata.runKeycloakInProdMode();
        this.certificateFormat = metadata.certificateFormat();
        // We want to set up sslEnable to same value as runKeycloakInProdMode as `isSslEnabled` is used to determine
        // if tls should be enabled
        this.sslEnabled = metadata.runKeycloakInProdMode();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;

        context.put(KEYCLOAK_PRODUCTION_MODE_KEY, runKeycloakInProdMode);

        if (runKeycloakInProdMode) {
            setUpProdKeycloak();
        }
        for (KeycloakContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new KeycloakGenericDockerContainerManagedResource(this);
    }

    private void setUpProdKeycloak() {
        if (certificateFormat.equals(Certificate.Format.JKS) || certificateFormat.equals(Certificate.Format.PKCS12)) {
            var keyStoreDestinationStrategy = new FixedPathContainerMountStrategy(null,
                    MOUNTED_KEYSTORE_PATH + getSuffixOfStore(certificateFormat), null, null);
            var cert = generateCertificate(keyStoreDestinationStrategy);
            cert.getKeyStoreConfigProperties().forEach(context::withTestScopeConfigProperty);
            enrichCommandByTlsCommands(false);
        } else if (certificateFormat.equals(Certificate.Format.PEM)) {
            var keyStoreDestinationStrategy = new FixedPathContainerMountStrategy(null, null,
                    MOUNTED_KEY_PATH, MOUNTED_CERT_PATH);
            var cert = generateCertificate(keyStoreDestinationStrategy);
            cert.getPemMountProperties().forEach(context::withTestScopeConfigProperty);
            enrichCommandByTlsCommands(true);
        } else {
            throw new IllegalArgumentException("Unsupported keystore format.");
        }
    }

    private io.quarkus.test.security.certificate.Certificate generateCertificate(
            FixedPathContainerMountStrategy keyStoreDestinationStrategy) {
        var trustStoreDestinationStrategy = new TrustStoreContainerMountStrategy();
        var destinationStrategy = new DelegatingContainerMountStrategy(keyStoreDestinationStrategy,
                trustStoreDestinationStrategy);

        var cert = io.quarkus.test.security.certificate.Certificate.of(KEYCLOAK, certificateFormat,
                KEYSTORE_PASSWORD, certTargetDir(), destinationStrategy, getSubjectAlternativeName());

        context.put(CERTIFICATE_CONTEXT_KEY, cert);

        return cert;
    }

    private void enrichCommandByTlsCommands(boolean isPemFormatUsed) {
        ArrayList<String> list = Arrays.stream(command)
                .collect(Collectors.toCollection(ArrayList::new));
        if (isPemFormatUsed) {
            list.add("--https-certificate-file=" + MOUNTED_CERT_PATH);
            list.add("--https-certificate-key-file=" + MOUNTED_KEY_PATH);
        } else {
            list.add("--https-key-store-file=" + MOUNTED_KEYSTORE_PATH + getSuffixOfStore(certificateFormat));
            list.add("--https-key-store-password=" + KEYSTORE_PASSWORD);
        }
        command = list.toArray(new String[0]);
    }

    private List<String> getSubjectAlternativeName() {
        OpenShiftClient openShiftClient = context.get(OpenShiftExtensionBootstrap.CLIENT);
        URL openShiftUrl = openShiftClient == null ? null : openShiftClient.getOpenShiftUrl();
        List<String> sans = new ArrayList<>();
        if (openShiftUrl == null) {
            sans.add("localhost");
        } else {
            // The SAN allow only have wildcard for lowest level (most left side) of subdomain
            sans.add(openShiftUrl.getHost().replace("api.", "*.apps."));
        }
        return sans;
    }

    private static Path certTargetDir() {
        return Path.of("target");
    }

    private static String getSuffixOfStore(Certificate.Format format) {
        return switch (format) {
            case PKCS12 -> ".p12";
            case JKS -> ".jks";
            default -> throw new IllegalArgumentException(format + " is not supported to get suffix.");
        };
    }

    private static final class TrustStoreContainerMountStrategy implements ContainerMountStrategy {

        @Override
        public String truststorePath(String currentLocation) {
            if (mountToContainer()) {
                return File.separator + "certs" + File.separator + getFileName(currentLocation);
            }
            return currentLocation;
        }

        private static String getFileName(String currentLocation) {
            return currentLocation.substring(currentLocation.lastIndexOf(File.separator) + 1);
        }

        @Override
        public String keystorePath(String currentLocation) {
            return currentLocation;
        }

        @Override
        public String keyPath(String currentLocation) {
            return currentLocation;
        }

        @Override
        public String certPath(String currentLocation) {
            return currentLocation;
        }

        @Override
        public boolean containerShareMountTrustStorePathWithApp() {
            return true;
        }

        @Override
        public boolean mountToContainer() {
            return isOpenshiftPlatform() || isKubernetesPlatform();
        }

        @Override
        public boolean trustStoreRequiresAbsolutePath() {
            return !mountToContainer();
        }
    }
}
