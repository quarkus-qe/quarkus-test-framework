package io.quarkus.test.bootstrap;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class RestService extends BaseService<RestService> {
    public RequestSpecification given() {
        return RestAssured.given().baseUri(getHost()).basePath("/").port(getPort());
    }

    public RequestSpecification https() {
        return RestAssured.given().baseUri(getHost(Protocol.HTTPS)).basePath("/").port(getPort(Protocol.HTTPS));
    }
}
