package io.quarkus.qe;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ValidateCustomProperty {

    public static final String DISALLOW_PROPERTY_VALUE = "WRONG!";
    public static final String CUSTOM_PROPERTY = "custom.property.name";

    private static final Logger LOG = Logger.getLogger(ValidateCustomProperty.class);

    @ConfigProperty(name = CUSTOM_PROPERTY)
    String value;

    void onStart(@Observes StartupEvent ev) {
        if (DISALLOW_PROPERTY_VALUE.equals(value)) {
            throw new RuntimeException("Wrong value! " + value);
        }
        LOG.info("App started with custom property: " + value);
    }
}
