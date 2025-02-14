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
}
