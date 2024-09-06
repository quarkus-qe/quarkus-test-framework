package io.quarkus.qe.hero;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("hero-client-resource")
public class HeroClientResource {

    @RestClient
    HeroClient heroClient;

    @GET
    public Hero triggerClientToServerCommunication() {
        return heroClient.getRandomHero();
    }

}
