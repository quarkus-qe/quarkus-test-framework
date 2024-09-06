package io.quarkus.test.bootstrap.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.bootstrap.AbstractCliCommand;
import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliCommandResult;
import io.quarkus.test.utils.FileUtils;

public class QuarkusConfigCommand extends AbstractCliCommand {

    public QuarkusConfigCommand(QuarkusCliClient cliClient) {
        super("config-command-test", "quarkus-config-command-tests", QuarkusCliClient.CreateApplicationRequest.defaults(),
                cliClient);
    }

    public QuarkusConfigCommand withSmallRyeConfigCryptoDep() {
        return addDependency("io.smallrye.config", "smallrye-config-crypto");
    }

    public QuarkusConfigCommand addDependency(String groupId, String artifactId) {
        var newDependency = """
                <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                </dependency>
                """.formatted(groupId, artifactId);
        var pom = this.app.getFileFromApplication("pom.xml");
        var updatedPom = FileUtils.loadFile(pom).transform(str -> {
            if (str.isEmpty()) {
                throw new IllegalArgumentException("The 'addDependency' method called before POM file was available");
            }
            var lastDependenciesClosingTag = str.lastIndexOf("</dependencies>");
            var result = str.substring(0, lastDependenciesClosingTag);
            result += newDependency;
            result += str.substring(lastDependenciesClosingTag);
            return result;
        });
        FileUtils.deleteFileContent(pom);
        FileUtils.copyContentTo(updatedPom, pom.toPath());
        return this;
    }

    public QuarkusConfigCommand addToApplicationPropertiesFile(String... properties) {
        if (properties.length % 2 != 0) {
            throw new IllegalArgumentException("The number of properties must be even.");
        }
        var propsMap = new HashMap<String, String>();
        String key = null;
        for (String p : properties) {
            if (key == null) {
                key = p;
            } else {
                propsMap.put(key, p);
                key = null;
            }
        }
        return addToApplicationPropertiesFile(propsMap);
    }

    public QuarkusConfigCommand addToApplicationPropertiesFile(Map<String, String> properties) {
        properties.forEach((propertyName, propertyValue) -> createProperty()
                .name(propertyName)
                .value(propertyValue)
                .executeCommand()
                .assertApplicationPropertiesContains(propertyName, propertyValue));
        return this;
    }

    public QuarkusSetConfigCommandBuilder createProperty() {
        return new QuarkusSetConfigCommandBuilder(false, this);
    }

    public QuarkusSetConfigCommandBuilder updateProperty() {
        return new QuarkusSetConfigCommandBuilder(true, this);
    }

    public QuarkusSetConfigCommandBuilder setProperty() {
        return new QuarkusSetConfigCommandBuilder(false, this);
    }

    public QuarkusRemoveConfigCommandBuilder removeProperty() {
        return new QuarkusRemoveConfigCommandBuilder(this);
    }

    public QuarkusEncryptConfigCommandBuilder encryptBuilder() {
        return new QuarkusEncryptConfigCommandBuilder(this);
    }

    QuarkusCliCommandResult runConfigCommand(List<String> subCmdArgs) {
        return runCommand("config", subCmdArgs);
    }

    @Override
    public AbstractCliCommand addToApplicationProperties(String... additions) {
        addToApplicationPropertiesFile(additions);
        return this;
    }
}
