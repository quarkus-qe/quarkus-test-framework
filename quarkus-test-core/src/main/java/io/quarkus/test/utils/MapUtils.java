package io.quarkus.test.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.StringUtils;

public final class MapUtils {

    private MapUtils() {

    }

    public static Map<String, String> difference(Map<String, String> newMap, Map<String, String> oldMap) {
        if (oldMap == null) {
            return newMap;
        }

        Map<String, String> differences = new HashMap<>();
        // Add new or changed entries
        for (Entry<String, String> newEntry : newMap.entrySet()) {
            String oldValue = oldMap.get(newEntry.getKey());
            if (oldValue == null || !StringUtils.equals(newEntry.getValue(), oldValue)) {
                differences.put(newEntry.getKey(), newEntry.getValue());
            }
        }

        // Add entries that are not in the new map
        for (Entry<String, String> oldEntry : oldMap.entrySet()) {
            if (!newMap.containsKey(oldEntry.getKey())) {
                differences.put(oldEntry.getKey(), oldEntry.getValue());
            }
        }

        return differences;
    }

}
