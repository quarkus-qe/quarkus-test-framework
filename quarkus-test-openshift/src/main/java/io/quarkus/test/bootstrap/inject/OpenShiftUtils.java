package io.quarkus.test.bootstrap.inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.opentest4j.AssertionFailedError;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

public final class OpenShiftUtils {
    private OpenShiftUtils() {
    }

    public static Optional<Deployment> getDeployment(List<HasMetadata> metadata) {
        for (HasMetadata metadatum : metadata) {
            if (metadatum instanceof Deployment) {
                return Optional.of((Deployment) metadatum);
            }
        }
        return Optional.empty();
    }

    public static String toYaml(List<HasMetadata> objects) {
        KubernetesList list = new KubernetesList();
        list.setItems(objects);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(Serialization.asYaml(list).getBytes());
            return os.toString();
        } catch (IOException e) {
            throw new AssertionFailedError("Failed adding properties into OpenShift template", e);
        }
    }

    /**
     * A valid label must be an empty string or consist of alphanumeric characters, '-', '_' or '.',
     * and must start and end with an alphanumeric character (e.g. 'MyValue', or 'my_value', or '12345',
     * regex used for validation is '(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?')
     *
     * @param labelValue label value
     * @return sanitized label value
     */
    public static String sanitizeLabelValue(String labelValue) {
        if (labelValue == null || labelValue.isEmpty()) {
            return labelValue;
        }
        if (labelValue.chars().allMatch(Character::isLetterOrDigit)) {
            return labelValue;
        }
        boolean isFirstLetter = true;
        var sanitizedValueBuilder = new StringBuilder();
        for (char c : labelValue.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (isFirstLetter) {
                    isFirstLetter = false;
                }
                sanitizedValueBuilder.append(c);
            } else if (!isFirstLetter) {
                sanitizedValueBuilder.append('_');
            }
        }

        for (int lastIndex = sanitizedValueBuilder.length() - 1; lastIndex >= 0; lastIndex--) {
            if (Character.isLetterOrDigit(sanitizedValueBuilder.charAt(lastIndex))) {
                break;
            }
            sanitizedValueBuilder.deleteCharAt(lastIndex);
        }

        return sanitizedValueBuilder.toString();
    }
}
