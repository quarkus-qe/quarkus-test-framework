package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;

import java.util.Arrays;
import java.util.List;

public class InfinispanService extends BaseService<InfinispanService> {

    public static final String USERNAME_DEFAULT = "my_username";
    public static final String PASSWORD_DEFAULT = "my_password";

    private String configFile;
    private List<String> userConfigFiles;
    private List<String> secretFiles;
    private String username = USERNAME_DEFAULT;
    private String password = PASSWORD_DEFAULT;

    public InfinispanService() {
        onPreStart(service -> {
            service.withProperty("USER", getUsername());
            service.withProperty("PASS", getPassword());

            if (configFile != null && !configFile.isEmpty()) {
                // legacy -> Infinispan previous to version 14
                service.withProperty("CONFIG_PATH", RESOURCE_PREFIX + configFile);
                // Infinispan 14+ configuration setup
                service.withProperty("INFINISPAN_CONFIG_PATH",
                        "resource_with_destination::/opt/infinispan/server/conf|" + configFile);
            }

            if (userConfigFiles != null) {
                for (int index = 0; index < userConfigFiles.size(); index++) {
                    service.withProperty("USER_CONFIG_" + index, RESOURCE_PREFIX + userConfigFiles.get(index));
                }
            }

            if (secretFiles != null) {
                for (int index = 0; index < secretFiles.size(); index++) {
                    service.withProperty("SECRET_" + index, SECRET_PREFIX + secretFiles.get(index));
                }
            }
        });
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getInfinispanServerAddress() {
        var uri = getURI(Protocol.HTTP);
        return uri.getHost() + ":" + uri.getPort();
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
}
