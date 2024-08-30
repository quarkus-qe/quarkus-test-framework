package io.quarkus.test.bootstrap.config;

import java.util.ArrayList;

import io.quarkus.test.bootstrap.QuarkusCliCommandResult;

public class QuarkusRemoveConfigCommandBuilder {

    private final QuarkusConfigCommand configCommand;
    private boolean help = false;
    private String propertyName;

    QuarkusRemoveConfigCommandBuilder(QuarkusConfigCommand configCommand) {
        this.configCommand = configCommand;
    }

    public QuarkusCliCommandResult printOutHelp() {
        this.help = true;
        return executeCommand();
    }

    public QuarkusRemoveConfigCommandBuilder name(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public QuarkusCliCommandResult executeCommand() {
        var subCommand = new ArrayList<String>();
        subCommand.add("remove");
        if (help) {
            subCommand.add("--help");
        } else {
            if (propertyName != null) {
                subCommand.add(propertyName);
            }
        }
        return configCommand.runConfigCommand(subCommand);
    }
}
