package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SLASH;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class InfinispanService extends BaseService<InfinispanService> {

    public static final String USERNAME_DEFAULT = "my_username";
    public static final String PASSWORD_DEFAULT = "my_password";

    private String configFile;
    private List<String> userConfigFiles;
    private List<String> secretFiles;
    private String username = USERNAME_DEFAULT;
    private String password = PASSWORD_DEFAULT;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getInfinispanServerAddress() {
        return getHost().replace("http://", "") + ":" + getPort();
    }

    public InfinispanService withConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }

    public InfinispanService withUserConfigFiles(String... userConfigFiles) {
        this.userConfigFiles = Arrays.asList(userConfigFiles);
        return this;
    }

    public InfinispanService withSecretFiles(String... secretFiles) {
        this.secretFiles = Arrays.asList(secretFiles);
        return this;
    }

    public InfinispanService withUsername(String username) {
        this.username = username;
        return this;
    }

    public InfinispanService withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public InfinispanService onPreStart(Action action) {
        withProperty("USER", getUsername());
        withProperty("PASS", getPassword());

        if (StringUtils.isNotEmpty(configFile)) {
            withProperty("CONFIG_PATH", RESOURCE_PREFIX + SLASH + configFile);
        }

        if (userConfigFiles != null) {
            for (int index = 0; index < userConfigFiles.size(); index++) {
                withProperty("USER_CONFIG_" + index, RESOURCE_PREFIX + SLASH + userConfigFiles.get(index));
            }
        }

        if (secretFiles != null) {
            for (int index = 0; index < secretFiles.size(); index++) {
                withProperty("SECRET_" + index, SECRET_PREFIX + SLASH + secretFiles.get(index));
            }
        }

        return super.onPreStart(action);
    }
}
