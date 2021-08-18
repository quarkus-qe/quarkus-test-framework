package io.quarkus.qe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;

@Path("/reactive-greeting")
public class ReactiveGreetingResource {

    public static final String PROPERTY = "custom.property.name";

    @ConfigProperty(name = PROPERTY)
    String name;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> sayHello() {
        return Uni.createFrom().item("Hello, I'm " + name);
    }
}
