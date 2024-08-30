package io.quarkus.test.bootstrap.tls;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import io.quarkus.test.bootstrap.AbstractCliCommand;
import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliCommandResult;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;

public final class QuarkusTlsCommand extends AbstractCliCommand {

    public QuarkusTlsCommand(QuarkusCliClient cliClient) {
        super("tls-command-test", getCreateAppReq(), cliClient, createTempDir());
    }

    @Override
    public QuarkusTlsCommand addToApplicationProperties(String... additions) {
        var currentAppPropsContent = getApplicationPropertiesAsString();
        var joinedAdditions = String.join(System.lineSeparator(), additions);
        var nextAppPropsContent = currentAppPropsContent + joinedAdditions;
        FileUtils.copyContentTo(nextAppPropsContent, getApplicationProperties().toPath());
        return this;
    }

    public QuarkusTlsGenerateCertificateCommand generateCertificate() {
        return new QuarkusTlsGenerateCertificateCommand(this);
    }

    public QuarkusTlsGenerateQuarkusCaCommand generateQuarkusCa() {
        return new QuarkusTlsGenerateQuarkusCaCommand(this);
    }

    QuarkusCliCommandResult runTlsCommand(List<String> subCmdArgs) {
        // TLS config command doesn't support -Dquarkus.analytics.disabled option
        // this forces QuarkusCliClient to don't set it
        try (var ignored = dontDisableAnalyticsWithProp()) {
            return runCommand("tls", subCmdArgs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static QuarkusCliClient.CreateApplicationRequest getCreateAppReq() {
        return QuarkusCliClient.CreateApplicationRequest.defaults()
                // TODO: we can drop 'tls-registry' when https://github.com/quarkusio/quarkus/issues/42751 is fixed
                .withExtensions("tls-registry", "quarkus-rest");
    }

    private static Closeable dontDisableAnalyticsWithProp() {
        // TODO: QE tracker for this workaround: https://issues.redhat.com/browse/QQE-935
        if (QuarkusProperties.disableBuildAnalytics()) {
            System.setProperty(QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, Boolean.FALSE.toString());
            return () -> System.setProperty(QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, Boolean.TRUE.toString());
        } else {
            // NOOP
            return () -> {
            };
        }
    }

    private static File createTempDir() {
        try {
            return Files.createTempDirectory("quarkus-tls-command-tests").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
