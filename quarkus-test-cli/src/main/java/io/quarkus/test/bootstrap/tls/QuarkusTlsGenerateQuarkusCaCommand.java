package io.quarkus.test.bootstrap.tls;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.bootstrap.QuarkusCliCommandResult;

public final class QuarkusTlsGenerateQuarkusCaCommand {

    public static final Path DEV_CA_TRUSTSTORE_PATH = Path.of(System.getenv("HOME")).resolve(".quarkus")
            .resolve("quarkus-dev-root-ca.pem");
    public static final String DEV_CA_PASSWORD = "quarkus";
    private final QuarkusTlsCommand quarkusTlsCommand;
    private final List<String> cmdOptions;

    QuarkusTlsGenerateQuarkusCaCommand(QuarkusTlsCommand quarkusTlsCommand) {
        this.quarkusTlsCommand = quarkusTlsCommand;
        this.cmdOptions = new ArrayList<>();
    }

    public QuarkusTlsGenerateQuarkusCaCommand withOption(GenerateQuarkusCaOptions optionConstant) {
        this.cmdOptions.add(optionConstant.option);
        return this;
    }

    public QuarkusCliCommandResult executeCommand() {
        var subCmdAndArgs = new ArrayList<String>();
        subCmdAndArgs.add("generate-quarkus-ca");
        subCmdAndArgs.addAll(cmdOptions);
        return quarkusTlsCommand.runTlsCommand(subCmdAndArgs);
    }
}
