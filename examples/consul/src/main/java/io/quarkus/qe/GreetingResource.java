package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

@Path("/api")
public class GreetingResource {

    @ConfigProperty(name = "my.property", defaultValue = "Default")
    String property;

    @ConfigProperty(name = "property.from.custom.source", defaultValue = "Default")
    String propertyFromCustomSource;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        return Uni.createFrom().item("Hello " + property);
    }

    @GET
    @Path("/from-custom-source")
    @Produces(MediaType.TEXT_PLAIN)
    public String propertyFromCustomSource() {
        return propertyFromCustomSource;
    }
}
