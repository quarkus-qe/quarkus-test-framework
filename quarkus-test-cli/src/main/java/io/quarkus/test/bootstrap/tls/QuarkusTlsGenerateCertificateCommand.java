package io.quarkus.test.bootstrap.tls;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.bootstrap.QuarkusCliCommandResult;

public final class QuarkusTlsGenerateCertificateCommand {

    private final QuarkusTlsCommand quarkusTlsCommand;
    private final List<String> cmdOptions;

    QuarkusTlsGenerateCertificateCommand(QuarkusTlsCommand quarkusTlsCommand) {
        this.quarkusTlsCommand = quarkusTlsCommand;
        this.cmdOptions = new ArrayList<>();
    }

    public QuarkusTlsGenerateCertificateCommand withOption(GenerateCertOptions optionConstant) {
        this.cmdOptions.add(optionConstant.option);
        return this;
    }

    public QuarkusTlsGenerateCertificateCommand withOption(GenerateCertOptions option, String value) {
        this.cmdOptions.addAll(option.toList(value));
        return this;
    }

    public QuarkusCliCommandResult executeCommand() {
        var subCmdAndArgs = new ArrayList<String>();
        subCmdAndArgs.add("generate-certificate");
        subCmdAndArgs.addAll(cmdOptions);
        return quarkusTlsCommand.runTlsCommand(subCmdAndArgs);
    }
}
