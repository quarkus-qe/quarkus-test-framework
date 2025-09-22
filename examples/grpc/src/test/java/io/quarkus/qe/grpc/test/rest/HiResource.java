package io.quarkus.qe.grpc.test.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("hi")
public class HiResource {

    @GET
    public String hi() {
        return "Hi Victor";
    }

}
