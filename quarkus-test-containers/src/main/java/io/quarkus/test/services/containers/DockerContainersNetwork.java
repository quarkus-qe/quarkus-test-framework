package io.quarkus.test.services.containers;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.Network;

public final class DockerContainersNetwork {
    private static volatile DockerContainersNetwork instance;
    private static Object mutex = new Object();
    private Map<String, Network> store = new HashMap<>();

    private DockerContainersNetwork() {

    }

    public static DockerContainersNetwork getInstance() {
        DockerContainersNetwork result = instance;
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null) {
                    instance = new DockerContainersNetwork();
                    result = instance;
                }
            }
        }
        return result;
    }

    /**
     * @getNetworkById returns a container network for a given ID, if the network doesn't exist then is created
     */
    public Network getNetworkByID(String id, NetworkType type) {
        String networkID = getFormattedId(id, type);
        if (!store.containsKey(networkID)) {
            if (type == NetworkType.SHARED) {
                store.put(networkID, Network.SHARED);
            } else {
                store.put(networkID, Network.newNetwork());
            }
        }

        return store.get(networkID);
    }

    private String getFormattedId(String id, NetworkType type) {
        return String.format("%s_%s", id, type.name());
    }

    public enum NetworkType {
        NEW,
        SHARED
    }
}
