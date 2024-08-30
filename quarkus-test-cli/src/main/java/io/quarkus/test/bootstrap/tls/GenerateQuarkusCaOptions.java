package io.quarkus.test.bootstrap.tls;

public enum GenerateQuarkusCaOptions {
    HELP_SHORT("-h"),
    HELP_LONG("--help"),
    INSTALL_SHORT("-i"),
    INSTALL_LONG("--install"),
    RENEW_SHORT("-r"),
    RENEW_LONG("--renew"),
    TRUSTSTORE_SHORT("-t"),
    TRUSTSTORE_LONG("--truststore");

    final String option;

    GenerateQuarkusCaOptions(String option) {
        this.option = option;
    }

}
