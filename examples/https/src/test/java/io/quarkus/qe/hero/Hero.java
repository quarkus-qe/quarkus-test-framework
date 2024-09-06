package io.quarkus.qe.hero;

public record Hero(Long id, String name, String otherName, int level, String picture, String powers) {
}
