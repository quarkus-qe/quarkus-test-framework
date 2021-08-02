package io.quarkus.qe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api")
public class GreetingResource {

    @ConfigProperty(name = "my.property", defaultValue = "Default")
    String property;

    @ConfigProperty(name = "property.from.custom.source", defaultValue = "Default")
    String propertyFromCustomSource;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello " + property;
    }

    @GET
    @Path("/from-custom-source")
    @Produces(MediaType.TEXT_PLAIN)
    public String propertyFromCustomSource() {
        return propertyFromCustomSource;
    }
}
