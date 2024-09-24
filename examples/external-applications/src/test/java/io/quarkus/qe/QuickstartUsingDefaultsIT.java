package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;

public abstract class QuickstartUsingDefaultsIT {

    protected abstract RestService getApp();

    @Test
    public void test() {
        getApp().given()
                .get("/hello")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
