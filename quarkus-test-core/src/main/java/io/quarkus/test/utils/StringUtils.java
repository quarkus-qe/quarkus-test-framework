package io.quarkus.test.utils;

import static io.quarkus.runtime.util.StringUtil.hyphenate;

public final class StringUtils {

    public static final String HYPHEN = "-";

    private StringUtils() {
    }

    /**
     * Kubernetes API objects like ConfigMap and Secret require that metadata name comply
     * the '[a-z0-9]([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*' regex.
     *
     * @param metadataName API object name as specified in 'metadata.name'
     * @return sanitized object name
     */
    public static String sanitizeKubernetesObjectName(String metadataName) {
        return hyphenate(metadataName);
    }

}
