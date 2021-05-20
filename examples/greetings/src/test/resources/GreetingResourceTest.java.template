package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class GreetingResourceTest {
    @Test
    public void shouldSayVictor() {
        RestAssured.given().get("/greeting").then().statusCode(HttpStatus.SC_OK);
    }
}
