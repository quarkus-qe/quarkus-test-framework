package io.quarkus.test.services;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class URILikeTest {

    @Test
    void serialization() {
        URILike uri = new URILike("http", "localhost", 1102, null);
        Assertions.assertEquals("http://localhost:1102", uri.toString());
    }

    @Test
    void emptyPort() {
        URILike uri = new URILike("http", "localhost", -1, null);
        Assertions.assertEquals("http://localhost", uri.toString());
    }

    @Test
    void emptyScheme() throws URISyntaxException {
        URILike ours = new URILike(null, "localhost", 8080, null);
        Assertions.assertEquals("localhost:8080", ours.toString());

        URI uri = new URI(null, "localhost", "/api", "bar");
        Assertions.assertEquals("//localhost/api#bar", uri.toString());
    }

    @Test
    void badScheme() {
        URILike uri = new URILike("SASL_PLAINTEXT", "0.0.0.0", 9092, null);
        Assertions.assertEquals("SASL_PLAINTEXT://0.0.0.0:9092", uri.toString());
        URILike updated = uri.withPort(9093);
        Assertions.assertEquals("SASL_PLAINTEXT://0.0.0.0:9093", updated.toString());
    }

    @Test
    void badSchemeParse() {
        URILike uriLike = URILike.parse("SASL_PLAINTEXT://0.0.0.0:9092");
        Assertions.assertEquals("SASL_PLAINTEXT", uriLike.getScheme());
        URILike updated = uriLike.withPort(9093);
        Assertions.assertEquals("SASL_PLAINTEXT://0.0.0.0:9093", updated.toString());

        Exception thrown = null;
        try {
            new URI("SASL_PLAINTEXT://0.0.0.0:9092");
            Assertions.fail("java.net.URI can not parse SASL_PLAINTEXT");
        } catch (Exception e) {
            thrown = e;
        }
        Assertions.assertNotNull(thrown);
        Assertions.assertInstanceOf(URISyntaxException.class, thrown);
    }

    @Test
    void debugParse() {
        URILike web = URILike.parse("https://quarkus.io/guides/mutiny-primer");
        Assertions.assertEquals("https", web.getScheme());
        Assertions.assertEquals("quarkus.io", web.getHost());
        Assertions.assertEquals(-1, web.getPort());
        Assertions.assertEquals("/guides/mutiny-primer", web.getPath());
        Assertions.assertEquals("https://quarkus.io/guides/mutiny-primer", web.toString());

        URILike full = URILike.parse("http://localhost:8087/auth/admin/master/console/");
        Assertions.assertEquals("http", full.getScheme());
        Assertions.assertEquals("localhost", full.getHost());
        Assertions.assertEquals(8087, full.getPort());
        Assertions.assertEquals("/auth/admin/master/console/", full.getPath());
        Assertions.assertEquals("http://localhost:8087/auth/admin/master/console/", full.toString());

        URILike deploy = URILike.parse("localhost:5000");
        Assertions.assertNull(deploy.getScheme());
        Assertions.assertEquals("localhost", deploy.getHost());
        Assertions.assertEquals(5000, deploy.getPort());
        Assertions.assertEquals("", deploy.getPath());
        Assertions.assertEquals("localhost:5000", deploy.toString());

        URILike bad = URILike.parse("SASL_PLAINTEXT://0.0.0.0:9092");
        Assertions.assertEquals("SASL_PLAINTEXT", bad.getScheme());
        Assertions.assertEquals("0.0.0.0", bad.getHost());
        Assertions.assertEquals(9092, bad.getPort());
        Assertions.assertEquals("SASL_PLAINTEXT://0.0.0.0:9092", bad.toString());
    }

    @Test
    void path() {
        final String serialised = "http://localhost:8087";
        final URILike ours = URILike.parse(serialised);
        final URI uri = URI.create(serialised);
        Assertions.assertEquals("", ours.getPath());
        Assertions.assertEquals("", uri.getPath());

        final URILike emptyPath = new URILike("http", "localhost", 8087, "");
        final URILike nullPath = new URILike("http", "localhost", 8087, null);
        Assertions.assertEquals(serialised, emptyPath.toString());
        Assertions.assertEquals(serialised, nullPath.toString());
    }
}
