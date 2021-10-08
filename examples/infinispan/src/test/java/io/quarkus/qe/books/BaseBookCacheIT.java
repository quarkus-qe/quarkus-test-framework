package io.quarkus.qe.books;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.restassured.http.ContentType;

@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Auto marshallers do not work on Quarkus 1.x")
public abstract class BaseBookCacheIT {

    private static final String BOOK_TITLE = "testBook";
    private static final Book BOOK = new Book(BOOK_TITLE, "description", 2011);

    @Test
    public void testBookResource() {
        given()
                .contentType(ContentType.JSON)
                .body(BOOK)
                .when().post("/book/add")
                .then().statusCode(HttpStatus.SC_NO_CONTENT);

        Book actual = given()
                .accept(ContentType.JSON)
                .when().get("/book/" + BOOK_TITLE)
                .as(Book.class);

        assertEquals(BOOK, actual);
    }
}
