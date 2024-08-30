package io.quarkus.test.bootstrap.config;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.QuarkusCliCommandResult;
import io.quarkus.test.logging.Log;
import io.quarkus.test.util.QuarkusCLIUtils;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.smallrye.common.os.OS;

public class QuarkusEncryptConfigCommandBuilder {

    public static final String AES_GCM_NO_PADDING_HANDLER_ENC_KEY = "smallrye.config.secret-handler.aes-gcm-"
            + "nopadding.encryption-key";
    private final QuarkusConfigCommand configCommand;
    private boolean help = false;
    private String secret = null;
    private boolean setEncryptionKeyToSecretHandler = true;
    private EncryptionKeyOpt encryptionKeyOpt = null;
    private EncryptionKeyFormatOpt encryptionKeyFormatOpt = null;
    private String encryptionKey = null;
    private KeyFormat encryptionKeyFormat = null;

    QuarkusEncryptConfigCommandBuilder(QuarkusConfigCommand configCommand) {
        this.configCommand = configCommand;
    }

    public QuarkusEncryptConfigCommandBuilder secret(String secret) {
        if (OS.WINDOWS.isCurrent()) {
            this.secret = QuarkusCLIUtils.escapeSecretCharsForWindows(secret);
        } else {
            this.secret = secret;
        }

        return this;
    }

    public QuarkusEncryptConfigCommandBuilder encryptionKeyOpt(EncryptionKeyOpt encryptionKeyOpt) {
        this.encryptionKeyOpt = encryptionKeyOpt;
        return this;
    }

    public QuarkusEncryptConfigCommandBuilder encryptionKeyFormatOpt(EncryptionKeyFormatOpt encryptionKeyFormatOpt) {
        this.encryptionKeyFormatOpt = encryptionKeyFormatOpt;
        return this;
    }

    public QuarkusEncryptConfigCommandBuilder encryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
        return this;
    }

    public QuarkusEncryptConfigCommandBuilder encryptionKeyFormat(KeyFormat encryptionKeyFormat) {
        this.encryptionKeyFormat = encryptionKeyFormat;
        return this;
    }

    public QuarkusEncryptConfigCommandBuilder doNotSetEncryptionKeyToSecretHandler() {
        // default handler won't be able to decode secret
        this.setEncryptionKeyToSecretHandler = false;
        return this;
    }

    public QuarkusCliCommandResult printOutHelp() {
        this.help = true;
        return executeCommand();
    }

    public QuarkusEncryptConfigCommandResult executeCommand() {
        var subCommand = new ArrayList<String>();
        subCommand.add("encrypt");
        if (help) {
            subCommand.add("--help");
        } else {
            if (encryptionKey != null) {
                subCommand.add(encryptionKeyOpt.option + "=" + encryptionKey);
                if (setEncryptionKeyToSecretHandler) {
                    configCommand.addToApplicationPropertiesFile(AES_GCM_NO_PADDING_HANDLER_ENC_KEY, encryptionKey);
                }
            } else if (encryptionKeyOpt != null) {
                subCommand.add(encryptionKeyOpt.option);
            }
            if (encryptionKeyFormat != null) {
                subCommand.add(encryptionKeyFormatOpt.option + "=" + encryptionKeyFormat);
            } else if (encryptionKeyFormatOpt != null) {
                subCommand.add(encryptionKeyFormatOpt.option);
            }
            if (secret != null) {
                subCommand.add(secret);
            }
        }
        return new QuarkusEncryptConfigCommandResult(configCommand.runConfigCommand(subCommand), configCommand);
    }

    public QuarkusConfigCommand getConfigCommand() {
        return configCommand;
    }

    public QuarkusEncryptConfigCommandBuilder withSmallRyeConfigSourceKeystoreDep() {
        configCommand.addDependency("io.smallrye.config", "smallrye-config-source-keystore");
        return this;
    }

    public QuarkusEncryptConfigCommandBuilder createKeyStore(String propertyName, String propertyValue, String storeName,
            String storePassword) {
        var resourceDir = configCommand.getApp().getServiceFolder().resolve("src").resolve("main").resolve("resources");
        var scriptName = storePassword + "-create-script";
        var script = resourceDir.resolve(scriptName);
        FileUtils.copyContentTo("""
                echo %s | keytool -importpass -alias %s -keystore %s -storepass %s -storetype PKCS12 -v
                """.formatted(propertyValue, propertyName, storeName, storePassword).trim(), script);
        try {
            new Command("chmod", "+x", "./" + scriptName).outputToConsole().onDirectory(resourceDir).runAndWait();
            new Command("./" + scriptName).outputToConsole().onDirectory(resourceDir).runAndWait();
        } catch (IOException | InterruptedException e) {
            // not failing as it has no effect for some reason
            Log.error("Failed to generate Keystore: " + e.getMessage());
        }
        if (!Files.exists(resourceDir.resolve(storeName))) {
            // not failing as it has no effect for some reason
            Log.error("Failed to generate Keystore - generated keystore is missing in directory " + resourceDir);
        }
        return this;
    }

    public static byte[] generateEncryptionKey() {
        try {
            return KeyGenerator.getInstance("AES").generateKey().getEncoded();
        } catch (Exception e) {
            Log.error("Error while generating the encryption key: ", e);
            Assertions.fail();
        }
        return null;
    }

    public enum EncryptionKeyOpt {
        SHORT("-k"),
        LONG("--key");

        private final String option;

        EncryptionKeyOpt(String option) {
            this.option = option;
        }
    }

    public enum EncryptionKeyFormatOpt {
        SHORT("-f"),
        LONG("--format");

        private final String option;

        EncryptionKeyFormatOpt(String option) {
            this.option = option;
        }
    }

    public enum KeyFormat {
        base64,
        /**
         * Basically says: key is already Base64, so do not encode it!
         */
        plain;

        public String format(byte[] key) {
            if (this == base64) {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
            } else {
                throw new IllegalStateException("Unsupported key format: " + this);
            }
        }
    }
}
