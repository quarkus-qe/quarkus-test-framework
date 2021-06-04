package io.quarkus.test.bootstrap;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class RestService extends BaseService<RestService> {

    private static final String BASE_PATH = "/";

    public RequestSpecification given() {
        return RestAssured.given().baseUri(getHost()).basePath(BASE_PATH).port(getPort());
    }

    public RequestSpecification https() {
        return RestAssured.given().baseUri(getHost(Protocol.HTTPS)).basePath("/").port(getPort(Protocol.HTTPS));
    }

    @Override
    public void start() {
        super.start();

        RestAssured.baseURI = getHost();
        RestAssured.basePath = BASE_PATH;
        RestAssured.port = getPort();
    }
}
