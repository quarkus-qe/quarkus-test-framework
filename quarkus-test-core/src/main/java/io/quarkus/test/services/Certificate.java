package io.quarkus.test.services;

/**
 * Defines certificate request for which this framework will generate a keystore and truststore.
 */
public @interface Certificate {

    enum Format {
        PEM,
        JKS,
        PKCS12
    }

    /**
     * Prefix keystore and truststore name with this attribute.
     */
    String prefix() default "quarkus-qe";

    /**
     * Secure file format.
     */
    Format format() default Format.PKCS12;

    /**
     * Keystore and truststore password.
     */
    String password() default "password";

    /**
     * Whether following configuration properties should be set for you:
     *
     * - `quarkus.http.ssl.certificate.key-store-file`
     * - `quarkus.http.ssl.certificate.key-store-file-type`
     * - `quarkus.http.ssl.certificate.key-store-password`
     *
     * You still can set and/or override these properties
     * with {@link io.quarkus.test.bootstrap.BaseService#withProperty(String, String)} service method.
     */
    boolean configureKeystore() default false;

    /**
     * Whether following configuration properties should be set for you:
     *
     * - `quarkus.http.ssl.certificate.trust-store-file`
     * - `quarkus.http.ssl.certificate.trust-store-file-type`
     * - `quarkus.http.ssl.certificate.trust-store-password`
     *
     * You still can set and/or override these properties
     * with {@link io.quarkus.test.bootstrap.BaseService#withProperty(String, String)} service method.
     */
    boolean configureTruststore() default false;
}
