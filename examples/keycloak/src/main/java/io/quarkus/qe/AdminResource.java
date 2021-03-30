package io.quarkus.qe;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/admin")
@RolesAllowed("test-admin-role")
public class AdminResource {
    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "Hello, admin " + identity.getPrincipal().getName();
    }

    @GET
    @Path("/issuer")
    @Produces(MediaType.TEXT_PLAIN)
    public String issuer() {
        return "admin token issued by " + jwt.getIssuer();
    }
}
