package io.quarkus.qe;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.qe.surefire.TlsCommandTest;
import io.quarkus.test.bootstrap.tls.GenerateCertOptions;
import io.quarkus.test.bootstrap.tls.GenerateQuarkusCaOptions;
import io.quarkus.test.bootstrap.tls.QuarkusTlsCommand;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersions;

@Disabled("https://github.com/quarkusio/quarkus/issues/51119")
@DisabledOnQuarkusVersions({
        // disable on 3.9-3.13
        @DisabledOnQuarkusVersion(version = "3\\.(9|10|11|12|13)\\..*", reason = "https://github.com/quarkusio/quarkus/issues/42752"),
        // disable on 3.14.0 and 3.14.1 as the fix is going to be backported to the next release
        @DisabledOnQuarkusVersion(version = "3\\.14\\.(0|1)", reason = "https://github.com/quarkusio/quarkus/issues/42752")
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("quarkus-cli")
@QuarkusScenario
public class QuarkusCliTlsCommandIT {

    @Inject
    static QuarkusTlsCommand tlsCommand;

    @Order(1)
    @Test
    public void generateQuarkusCa() {
        tlsCommand
                .generateQuarkusCa()
                .withOption(GenerateQuarkusCaOptions.TRUSTSTORE_LONG)
                .withOption(GenerateQuarkusCaOptions.RENEW_SHORT)
                .executeCommand()
                .assertCommandOutputContains("Generating Quarkus Dev CA certificate")
                .assertCommandOutputContains("Truststore generated successfully")
                .assertFileExistsStr(cmd -> cmd.getOutputLineRemainder("Truststore generated successfully:"));
    }

    @Order(2)
    @Test
    public void generateCertificate() {
        // prepares state for assertion in TlsCommandTest
        var appSvcDir = tlsCommand.getApp().getServiceFolder().toAbsolutePath().toString();
        tlsCommand
                .generateCertificate()
                .withOption(GenerateCertOptions.COMMON_NAME_SHORT, "Dumbledore")
                .withOption(GenerateCertOptions.NAME_LONG, "dev-certificate")
                .withOption(GenerateCertOptions.PASSWORD_LONG, "quarkus")
                .withOption(GenerateCertOptions.DIRECTORY_SHORT, appSvcDir)
                .executeCommand()
                .assertCommandOutputContains("Quarkus Dev CA certificate found at")
                .assertCommandOutputContains("PKCS12 keystore and truststore generated successfully!")
                .assertFileExistsStr(cmd -> cmd.getOutputLineRemainder("Key Store File:"))
                .assertFileExistsStr(cmd -> cmd.getOutputLineRemainder("Trust Store File:"))
                .addToAppProps(cmd -> {
                    // move to application properties under test profile
                    var key = "%dev.quarkus.tls.key-store.p12.path";
                    var val = cmd.getPropertyValueFromEnvFile(key);
                    return "%test.quarkus.tls.key-store.p12.path=" + val;
                })
                .addToAppProps(cmd -> {
                    // move to application properties under test profile
                    var key = "%dev.quarkus.tls.key-store.p12.password";
                    var val = cmd.getPropertyValueFromEnvFile(key);
                    return "%test.quarkus.tls.key-store.p12.password=" + val;
                });
    }

    @Order(3)
    @Test
    public void runTestsUsingGeneratedCerts() {
        // runs TlsCommandTest that verifies cert generation
        tlsCommand.buildAppAndExpectSuccess(TlsCommandTest.class);
    }
}
