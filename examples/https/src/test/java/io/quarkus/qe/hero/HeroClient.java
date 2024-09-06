package io.quarkus.qe.hero;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hero")
public interface HeroClient {

    @GET
    @Path("/api/heroes/random")
    Hero getRandomHero();

}
