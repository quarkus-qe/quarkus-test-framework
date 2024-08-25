package io.quarkus.test.bootstrap.config;

import static io.quarkus.test.bootstrap.config.QuarkusEncryptConfigCommandBuilder.AES_GCM_NO_PADDING_HANDLER_ENC_KEY;

import java.util.Objects;
import java.util.function.Consumer;

import io.quarkus.test.util.QuarkusCLIUtils;

public class QuarkusEncryptConfigCommandResult extends QuarkusConfigCommandResult {

    private static final String SECRET_ENCRYPTED_TO = "was encrypted to";
    private static final String WITH_GENERATED_KEY = "with the generated encryption key";
    private final QuarkusConfigCommand configCommand;
    private String encryptedSecret = null;

    QuarkusEncryptConfigCommandResult(QuarkusConfigCommandResult delegate, QuarkusConfigCommand configCommand) {
        super(delegate.output, delegate.applicationPropertiesAsString);
        this.configCommand = configCommand;
    }

    public String getGeneratedEncryptionKey() {
        if (output.contains(WITH_GENERATED_KEY)) {
            return output
                    .transform(o -> o.substring(o.lastIndexOf(" ")))
                    .transform(QuarkusCLIUtils::toUtf8)
                    .transform(QuarkusCLIUtils::removeAnsiAndHiddenChars)
                    .trim();
        }
        return null;
    }

    public String getEncryptedSecret() {
        if (encryptedSecret == null) {
            encryptedSecret = output
                    .transform(o -> o.split(SECRET_ENCRYPTED_TO)[1])
                    .transform(remaining -> remaining.split(WITH_GENERATED_KEY)[0])
                    .transform(QuarkusCLIUtils::toUtf8)
                    .transform(QuarkusCLIUtils::removeAnsiAndHiddenChars)
                    .trim();
        }
        return encryptedSecret;
    }

    public QuarkusEncryptConfigCommandResult secretConsumer(Consumer<String> secretConsumer) {
        secretConsumer.accept(getEncryptedSecret());
        return this;
    }

    public QuarkusEncryptConfigCommandResult storeSecretAsSecretExpression(String propertyName) {
        configCommand.addToApplicationPropertiesFile(propertyName, withDefaultSecretKeyHandler(getEncryptedSecret()));
        return this;
    }

    public QuarkusEncryptConfigCommandResult storeSecretAsRawValue(String propertyName) {
        configCommand.addToApplicationPropertiesFile(propertyName, getEncryptedSecret());
        return this;
    }

    public QuarkusEncryptConfigCommandResult storeGeneratedKeyAsProperty() {
        var generatedEncryptionKey = getGeneratedEncryptionKey();
        Objects.requireNonNull(generatedEncryptionKey);
        configCommand.addToApplicationPropertiesFile(AES_GCM_NO_PADDING_HANDLER_ENC_KEY, generatedEncryptionKey);
        return this;
    }

    public QuarkusEncryptConfigCommandResult generatedKeyConsumer(Consumer<String> encKeyConsumer) {
        encKeyConsumer.accept(getGeneratedEncryptionKey());
        return this;
    }

    public static String withDefaultSecretKeyHandler(String secret) {
        return "${aes-gcm-nopadding::%s}".formatted(secret);
    }
}
