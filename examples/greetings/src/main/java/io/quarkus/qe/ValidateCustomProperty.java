package io.quarkus.qe;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ValidateCustomProperty {

    public static final String DISALLOW_PROPERTY_VALUE = "WRONG!";
    public static final String CUSTOM_PROPERTY = "custom.property.name";

    @ConfigProperty(name = CUSTOM_PROPERTY)
    String value;

    void onStart(@Observes StartupEvent ev) {
        if (DISALLOW_PROPERTY_VALUE.equals(value)) {
            throw new RuntimeException("Wrong value! " + value);
        }
    }
}
