package io.quarkus.test.openshift.utils;

import static io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap.CLIENT;
import static io.quarkus.test.security.certificate.ServingCertificateConfig.isServingCertificateScenario;

import java.util.random.RandomGenerator;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.security.certificate.ServingCertificateConfig;
import io.quarkus.test.services.URILike;

public final class OpenShiftPropertiesUtils {

    public record PropertyToValue(String key, String value) {
    }

    public record PropertyToObj(String key, PropertyToValue value) {
    }

    public static final int EXTERNAL_SSL_PORT = 443;
    /**
     * ConfigMap name we use for OpenShift CA certificate injection.
     */
    public static final String CA_BUNDLE_CONFIGMAP_NAME = "ca-bundle-configmap";
    /**
     * Secret name we use for OpenShift serving certificates.
     */
    public static final String SERVING_CERTS_SECRET_NAME = "serving-certificates-secret";
    /**
     * Adds service annotation. Adding annotations is currently implemented for Quarkus runtimes only as that's what we
     * need. Supporting for all runtimes will require additional test that annotation is added to one service only.
     */
    private static final String ANNOTATION_PREFIX = "annotation::";
    /**
     * Separates annotation name and value.
     */
    private static final String ANNOTATION_SEPARATOR = "|";
    /**
     * Mounts existing secret. Mounting secrets is currently implemented for Quarkus runtimes only as that's what we
     * need. Supporting for all runtimes will require additional test that secret is mounted to one service only.
     */
    private static final String MOUNT_SECRET_PREFIX = "mount-secret::";
    /**
     * Separates secret name and mount path.
     */
    private static final String MOUNT_SECRET_SEPARATOR = "|";
    /**
     * Mounts existing configmap. Mounting configmaps is currently implemented for Quarkus runtimes only as that's
     * what we need. Supporting for all runtimes will require additional test that secret is mounted to one service only.
     */
    private static final String MOUNT_CONFIGMAP_PREFIX = "mount-configmap::";
    /**
     * Separates configmap name and mount path.
     */
    private static final String MOUNT_CONFIGMAP_SEPARATOR = "|";
    /**
     * Creates empty config map with annotation.
     */
    private static final String ANNOTATED_CONFIG_MAP_PREFIX = "annotated-configmap::";
    /**
     * Separates configmap name and annotation.
     */
    private static final String ANNOTATED_CONFIG_MAP_SEPARATOR = "|";
    /**
     * Separates configmap annotation key and value.
     */
    private static final String ANNOTATED_CONFIG_ANNOTATION_SEPARATOR = "&";
    private static final int INTERNAL_HTTPS_PORT_DEFAULT = 8443;
    private static final String QUARKUS_HTTPS_PORT_PROPERTY = "quarkus.http.ssl-port";

    private OpenShiftPropertiesUtils() {
        // utils
    }

    public static String buildAnnotatedConfigMapProp(String configmapName, String annotationKey, String annotationVal) {
        return ANNOTATED_CONFIG_MAP_PREFIX + configmapName + ANNOTATED_CONFIG_MAP_SEPARATOR + annotationKey
                + ANNOTATED_CONFIG_ANNOTATION_SEPARATOR + annotationVal;
    }

    public static String buildMountConfigMapProp(String secretName, String secretValue) {
        return MOUNT_CONFIGMAP_PREFIX + secretName + MOUNT_CONFIGMAP_SEPARATOR + secretValue;
    }

    public static String buildMountSecretProp(String secretName, String secretValue) {
        return MOUNT_SECRET_PREFIX + secretName + MOUNT_SECRET_SEPARATOR + secretValue;
    }

    public static String buildAnnotationProp(String annotationName, String annotationValue) {
        return ANNOTATION_PREFIX + annotationName + ANNOTATION_SEPARATOR + annotationValue;
    }

    public static boolean isMountSecret(String propertyValue) {
        return propertyValue.startsWith(MOUNT_SECRET_PREFIX);
    }

    public static boolean isMountConfigMap(String propertyValue) {
        return propertyValue.startsWith(MOUNT_CONFIGMAP_PREFIX);
    }

    public static boolean isAnnotatedConfigMap(String propertyValue) {
        return propertyValue.startsWith(ANNOTATED_CONFIG_MAP_PREFIX);
    }

    public static boolean isAnnotation(String propertyValue) {
        return propertyValue.startsWith(ANNOTATION_PREFIX);
    }

    public static String createAnnotationPropertyKey() {
        return createRandomPropertyKeyWithPrefix("annotation-");
    }

    public static String createSecretPropertyKey() {
        return createRandomPropertyKeyWithPrefix("secret-");
    }

    public static String createConfigMapPropertyKey() {
        return createRandomPropertyKeyWithPrefix("configmap-");
    }

    public static String createAnnotatedConfigMapPropertyKey() {
        return createRandomPropertyKeyWithPrefix("annotated-configmap-");
    }

    public static PropertyToValue getMountConfigMap(String propertyValue) {
        return getPropertyToValue(propertyValue, MOUNT_CONFIGMAP_PREFIX, MOUNT_CONFIGMAP_SEPARATOR, "configmap");
    }

    public static PropertyToValue getMountSecret(String propertyValue) {
        return getPropertyToValue(propertyValue, MOUNT_SECRET_PREFIX, MOUNT_SECRET_SEPARATOR, "secret");
    }

    public static PropertyToValue getServiceAnnotation(String propertyValue) {
        return getPropertyToValue(propertyValue, ANNOTATION_PREFIX, ANNOTATION_SEPARATOR, "annotation");
    }

    public static PropertyToObj getAnnotatedConfigMap(String propertyValue) {
        var configMapNameToAnnotation = getPropertyToValue(propertyValue, ANNOTATED_CONFIG_MAP_PREFIX,
                ANNOTATED_CONFIG_MAP_SEPARATOR, "annotated-configmap");
        var annotationKeyToValue = getPropertyToValue(configMapNameToAnnotation.value(), "",
                ANNOTATED_CONFIG_ANNOTATION_SEPARATOR, "annotation-configmap-annotation");
        return new PropertyToObj(configMapNameToAnnotation.key(), annotationKeyToValue);
    }

    public static boolean checkPodReadinessWithStatusInsteadOfRoute(ServiceContext ctx) {
        // using HTTPS & internal service URL & no exposed route => need to trust readiness
        return isServingCertificateScenario(ctx) && ServingCertificateConfig.get(ctx).addServiceCertificate();
    }

    public static int getInternalHttpsPort(ServiceContext ctx) {
        return ctx.getOwner().getProperty(QUARKUS_HTTPS_PORT_PROPERTY)
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .orElse(INTERNAL_HTTPS_PORT_DEFAULT);
    }

    public static URILike getInternalServiceHttpsUrl(ServiceContext ctx) {
        var serviceName = ctx.getOwner().getName();
        var projectName = ctx.<OpenShiftClient> get(CLIENT).project();
        var host = "%s.%s.svc.cluster.local".formatted(serviceName, projectName);
        return new URILike(Protocol.HTTPS.getValue(), host, getInternalHttpsPort(ctx), "");
    }

    private static PropertyToValue getPropertyToValue(String propertyValue, String prefix, String separator, String subject) {
        var keyToVal = propertyValue.replace(prefix, StringUtils.EMPTY);
        var separatorIdx = keyToVal.indexOf(separator);
        if (separatorIdx == -1) {
            Assertions.fail("Configuration property defining OpenShift '%s' is missing key to value separator '%s'"
                    .formatted(subject, separatorIdx));
        }
        var key = keyToVal.substring(0, separatorIdx);
        var value = keyToVal.substring(separatorIdx + 1);
        return new PropertyToValue(key, value);
    }

    private static String createRandomPropertyKeyWithPrefix(String x) {
        var generator = RandomGenerator.getDefault();
        return x + generator.nextInt() + "-" + generator.nextInt();
    }
}
