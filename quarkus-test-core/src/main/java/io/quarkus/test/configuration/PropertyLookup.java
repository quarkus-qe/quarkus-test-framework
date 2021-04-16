package io.quarkus.test.configuration;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.ServiceContext;

public class PropertyLookup {

    private final String propertyKey;
    private final String defaultValue;

    public PropertyLookup(String propertyKey) {
        this(propertyKey, StringUtils.EMPTY);
    }

    public PropertyLookup(String propertyKey, String defaultValue) {
        this.propertyKey = propertyKey;
        this.defaultValue = defaultValue;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String get(ServiceContext service) {
        // From Store
        String value = service.get(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        // Or from test.properties
        value = service.getOwner().getConfiguration().get(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        // Or from service properties
        value = service.getOwner().getProperties().get(value);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        // Or from system properties
        return get();
    }

    public String get() {
        String value = System.getProperty(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return defaultValue;
    }
}
