package io.quarkus.test.bootstrap;

import org.apache.commons.lang3.StringUtils;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class RestService extends BaseService<RestService> {

    private static final String BASE_PATH = "/";

    private WebClient webClient;

    public RequestSpecification given() {
        return RestAssured.given().baseUri(getHost()).basePath(BASE_PATH).port(getPort());
    }

    public RequestSpecification https() {
        return RestAssured.given().baseUri(getHost(Protocol.HTTPS)).basePath("/").port(getPort(Protocol.HTTPS));
    }

    public WebClient mutiny() {
        return mutiny(new WebClientOptions());
    }

    public WebClient mutiny(WebClientOptions options) {
        if (webClient == null) {
            webClient = WebClient.create(Vertx.vertx(), options
                    .setDefaultHost(getHost().replace("http://", StringUtils.EMPTY))
                    .setDefaultPort(getPort()));
        }

        return webClient;
    }

    @Override
    public void start() {
        super.start();

        RestAssured.baseURI = getHost();
        RestAssured.basePath = BASE_PATH;
        RestAssured.port = getPort();
    }
}
