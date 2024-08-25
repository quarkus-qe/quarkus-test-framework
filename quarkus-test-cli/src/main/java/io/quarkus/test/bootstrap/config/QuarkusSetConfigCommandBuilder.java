package io.quarkus.test.bootstrap.config;

import java.util.ArrayList;

import io.quarkus.test.util.QuarkusCLIUtils;
import io.smallrye.common.os.OS;

public class QuarkusSetConfigCommandBuilder {

    private final boolean updateScenario;
    private final QuarkusConfigCommand configCommand;
    private String propertyName = null;
    private String propertyValue = null;
    private boolean encrypt = false;
    private EncryptOption encryptOption = EncryptOption.LONG;
    private boolean help = false;

    QuarkusSetConfigCommandBuilder(boolean updateScenario, QuarkusConfigCommand configCommand) {
        this.updateScenario = updateScenario;
        this.configCommand = configCommand;
    }

    public QuarkusSetConfigCommandBuilder name(String name) {
        this.propertyName = name;
        return this;
    }

    public QuarkusSetConfigCommandBuilder value(String value) {
        if (OS.WINDOWS.isCurrent()) {
            this.propertyValue = QuarkusCLIUtils.escapeSecretCharsForWindows(value);
        } else {
            this.propertyValue = value;
        }
        return this;
    }

    public QuarkusConfigCommandResult printOutHelp() {
        this.help = true;
        return executeCommand();
    }

    public QuarkusSetConfigCommandBuilder encrypt() {
        return encrypt(EncryptOption.LONG);
    }

    public QuarkusSetConfigCommandBuilder encrypt(EncryptOption encryptOption) {
        this.encrypt = true;
        this.encryptOption = encryptOption;
        return this;
    }

    public QuarkusConfigCommandResult executeCommand() {
        // e.g. quarkus config set --encrypt --name=my.secret --value=1234
        var subCommand = new ArrayList<String>();
        subCommand.add("set");
        if (help) {
            subCommand.add("--help");
        } else {
            if (!updateScenario && propertyValue == null) {
                throw new IllegalStateException("No property value specified for create property scenario.");
            }
            if (encrypt) {
                subCommand.add(encryptOption.option);
            }
            if (propertyName != null) {
                subCommand.add(propertyName);
            }
            if (propertyValue != null) {
                subCommand.add(propertyValue);
            }
        }
        return configCommand.runConfigCommand(subCommand);
    }

    public enum EncryptOption {
        SHORT("-k"),
        LONG("--encrypt");

        final String option;

        EncryptOption(String option) {
            this.option = option;
        }
    }
}
