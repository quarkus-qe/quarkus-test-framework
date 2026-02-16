package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting")
public class GreetingResource {

    @ConfigProperty(name = "user")
    String user;

    @ConfigProperty(name = "password")
    String password;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello World!";
    }

    @GET
    @Path("/credentials")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCredentials() {
        return "User: " + user + ", Password: " + password;
    }
}
