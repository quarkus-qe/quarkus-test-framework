package io.quarkus.test.bootstrap;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class RestService extends BaseService<RestService> {

    private static final String BASE_PATH = "/";

    private WebClient webClient;

    public RequestSpecification given() {
        var host = getURI(Protocol.HTTP);
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification https() {
        Protocol protocol = Protocol.HTTPS;
        var host = getURI(protocol);
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification management() {
        var host = getURI(Protocol.MANAGEMENT);
        if (host.getScheme().equals(Protocol.MANAGEMENT.getValue())) {
            throw new IllegalArgumentException("Can not find URL to the management interface");
        }
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification relaxedHttps() {
        return this.https().relaxedHTTPSValidation();
    }

    public WebClient mutiny() {
        return mutiny(new WebClientOptions());
    }

    public WebClient mutiny(WebClientOptions options) {
        if (webClient == null) {
            var uri = getURI(Protocol.HTTP);
            webClient = WebClient.create(Vertx.vertx(), options
                    .setDefaultHost(uri.getHost())
                    .setDefaultPort(uri.getPort()));
        }

        return webClient;
    }

    @Override
    public void start() {
        super.start();
        var host = getURI(Protocol.HTTP);
        RestAssured.baseURI = host.getRestAssuredStyleUri();
        RestAssured.basePath = BASE_PATH;
        RestAssured.port = host.getPort();
    }
}
