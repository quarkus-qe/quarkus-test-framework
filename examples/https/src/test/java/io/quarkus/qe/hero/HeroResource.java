package io.quarkus.qe.hero;

import java.util.random.RandomGenerator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/heroes/random")
public class HeroResource {

    @GET
    public Hero getRandomHero() {
        long random = RandomGenerator.getDefault().nextLong();
        return new Hero(random, "Name-" + random, "Other-" + random, 1, "placeholder", "root");
    }

}
