package io.quarkus.test.model;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

/**
 * CustomVolume represents a K8s volume plus some additions related to how is mounted.
 */
public class CustomVolume {
    public enum VolumeType {
        CONFIG_MAP,
        SECRET
    }

    private final Volume volume;
    private final String name;
    private final String subFolderRegExp;
    private ConfigMapVolumeSource configMap;
    private SecretVolumeSource secret;

    public CustomVolume(String name, String subFolderRegExp, VolumeType volumeType) {
        this.subFolderRegExp = subFolderRegExp;
        this.name = name;
        VolumeBuilder vBuilder = new VolumeBuilder().withName(name);

        if (volumeType.equals(VolumeType.CONFIG_MAP)) {
            this.configMap = new ConfigMapVolumeSourceBuilder().withName(name).build();
            vBuilder = vBuilder.withConfigMap(configMap);
        } else {
            this.secret = new SecretVolumeSourceBuilder().withSecretName(name).build();
            vBuilder = vBuilder.withSecret(secret);
        }

        volume = vBuilder.build();
    }

    public Volume getVolume() {
        return volume;
    }

    public String getName() {
        return name;
    }

    public ConfigMapVolumeSource getConfigMap() {
        return configMap;
    }

    public void setConfigMap(ConfigMapVolumeSource configMap) {
        this.configMap = configMap;
    }

    public SecretVolumeSource getSecret() {
        return secret;
    }

    public void setSecret(SecretVolumeSource secret) {
        this.secret = secret;
    }

    public String getSubFolderRegExp() {
        return subFolderRegExp;
    }
}
