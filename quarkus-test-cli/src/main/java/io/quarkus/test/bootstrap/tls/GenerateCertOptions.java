package io.quarkus.test.bootstrap.tls;

import java.util.List;

import io.quarkus.test.util.QuarkusCLIUtils;
import io.smallrye.common.os.OS;

public enum GenerateCertOptions {
    COMMON_NAME_SHORT("-c"),
    COMMON_NAME_LONG("--cn="),
    DIRECTORY_SHORT("-d"),
    DIRECTORY_LONG("--directory="),
    HELP_SHORT("-h"),
    HELP_LONG("--help"),
    NAME_SHORT("-n"),
    NAME_LONG("--name="),
    PASSWORD_SHORT("-p"),
    PASSWORD_LONG("--password="),
    RENEW_SHORT("-r"),
    RENEW_LONG("--renew");

    final String option;

    GenerateCertOptions(String option) {
        this.option = option;
    }

    List<String> toList(String value) {
        final String normalizedVal;
        if (OS.WINDOWS.isCurrent()) {
            normalizedVal = QuarkusCLIUtils.escapeSecretCharsForWindows(value);
        } else {
            normalizedVal = value;
        }
        // equals sign means no space, short options require space
        if (COMMON_NAME_LONG == this || DIRECTORY_LONG == this || NAME_LONG == this || PASSWORD_LONG == this) {
            return List.of(option + normalizedVal);
        } else {
            return List.of(option, normalizedVal);
        }
    }
}
