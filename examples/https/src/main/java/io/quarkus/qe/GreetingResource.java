package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.Authenticated;

@Path("/greeting")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello World!";
    }

    @Authenticated
    @Path("/mutual-tls")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHelloClient(@Context SecurityContext securityContext) {
        return "Hello %s!".formatted(securityContext.getUserPrincipal().getName());
    }
}
