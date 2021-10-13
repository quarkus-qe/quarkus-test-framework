package io.quarkus.test.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class URILikeTest {

    @Test
    void string() {
        URILike uri = new URILike("http", "localhost", 1102, null);
        Assertions.assertEquals("http://localhost:1102", uri.toString());
    }
}
