package io.quarkus.test.services;

import java.net.URI;
import java.net.URISyntaxException;

public class URILike {

    private static final String SCHEME_SEPARATOR = ":";
    private static final String HOST_PREFIX = "//";
    /*
     * Things like "SASL_PLAINTEXT" are not valid schemes from the URI point of view,
     * but we need to process them anyway
     */
    private final String scheme;
    private final URI wrapped;

    public URILike(String scheme, String userinfo, String host, int port, String path) {
        this.scheme = scheme;
        this.wrapped = createURI(
                null, //we process scheme separately
                userinfo,
                host,
                port,
                path,
                null, // query is not used at the time of writing
                null // fragment is not used at the time of writing
        );
    }

    public URILike(String scheme, String host, int port, String path) {
        this(scheme, null, host, port, path);
    }

    private URILike(String scheme, URI uri) {
        this.scheme = scheme;
        this.wrapped = uri;
    }

    /**
     * Fluently creates new object, but replaces "scheme" part.
     *
     * @param scheme — String, will be used as a "scheme" part in the resulting string.
     *        May not be conforming to https://datatracker.ietf.org/doc/html/rfc3986/#section-3.1
     * @return new object with changed value of "scheme"
     */
    public URILike withScheme(String scheme) {
        return new URILike(scheme, this.wrapped);
    }

    public URILike withPath(String path) {
        URI wrapped = this.wrapped;
        var withPath = createURI(wrapped.getScheme(),
                wrapped.getUserInfo(),
                wrapped.getHost(),
                wrapped.getPort(),
                path,
                wrapped.getQuery(),
                wrapped.getFragment());
        return new URILike(this.scheme, withPath);
    }

    public URILike withPort(int port) {
        URI wrapped = this.wrapped;
        var withPath = createURI(wrapped.getScheme(),
                wrapped.getUserInfo(),
                wrapped.getHost(),
                port,
                wrapped.getPath(),
                wrapped.getQuery(),
                wrapped.getFragment());
        return new URILike(this.scheme, withPath);
    }

    public URILike withQuery(String query) {
        URI wrapped = this.wrapped;
        var withPath = createURI(wrapped.getScheme(),
                wrapped.getUserInfo(),
                wrapped.getHost(),
                wrapped.getPort(),
                wrapped.getPath(),
                query,
                wrapped.getFragment());
        return new URILike(this.scheme, withPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(SCHEME_SEPARATOR);
        }
        sb.append(wrapped);
        String result = sb.toString();
        if (result.startsWith("//")) {
            return result.substring(2);
        } else {
            return result;
        }
    }

    public String getScheme() {
        return this.scheme;
    }

    public String getHost() {
        return wrapped.getHost();
    }

    public int getPort() {
        return wrapped.getPort();
    }

    /**
     * @return "URI" formatted in style, preferred by RestAssured library(e.g. "http://localhost")
     */
    public String getRestAssuredStyleUri() {
        return scheme + SCHEME_SEPARATOR + HOST_PREFIX + wrapped.getHost();
    }

    public String getUserInfo() {
        return wrapped.getUserInfo();
    }

    public String getPath() {
        return wrapped.getPath();
    }

    public static URILike parse(String uri) {
        final String[] schemeAndTheRest = uri.split("://");
        final boolean hasScheme = schemeAndTheRest.length == 2; //  [0]: scheme + :// + [1]: rest\

        //java.net.URI expects a scheme and requires it to not contain underscores. This is a workaround
        final URI parsed;
        final String realScheme;
        final String fakeScheme = "kludge://";
        if (!hasScheme) {
            parsed = URI.create(fakeScheme + uri);
            realScheme = null;
        } else if (schemeAndTheRest[0].contains("_")) {
            parsed = URI.create(fakeScheme + schemeAndTheRest[1]);
            realScheme = schemeAndTheRest[0];
        } else {
            parsed = URI.create(uri);
            realScheme = parsed.getScheme();
        }

        return new URILike(realScheme,
                parsed.getUserInfo(),
                parsed.getHost(),
                parsed.getPort(),
                parsed.getPath());
    }

    private static URI createURI(String scheme,
            String userInfo,
            String host,
            int port,
            String path,
            String query,
            String fragment) {
        try {
            return new URI(
                    scheme,
                    userInfo,
                    host,
                    port,
                    path,
                    query,
                    fragment);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
