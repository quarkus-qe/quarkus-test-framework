package io.quarkus.qe;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting")
public class GreetingResource {

    @ConfigProperty(name = ValidateCustomProperty.CUSTOM_PROPERTY)
    String name;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello, I'm " + name;
    }

    @GET
    @Path("/file")
    @Produces(MediaType.TEXT_PLAIN)
    public String text() throws IOException {
        return new String(GreetingResource.class.getResourceAsStream("/custom/text.txt").readAllBytes());
    }
}
