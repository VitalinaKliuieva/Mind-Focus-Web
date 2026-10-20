package com.mindfocus.qa.support;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Request builders. Every call is attached to the Allure report with its request and response. */
public final class Api {
    private Api() {}

    /** A request without a token. */
    public static RequestSpecification anon() {
        return RestAssured.given()
                .baseUri(Settings.API_URL)
                .contentType(ContentType.JSON)
                .filter(new AllureRestAssured());
    }

    /** A request with a bearer token. */
    public static RequestSpecification as(String token) {
        return anon().header("Authorization", "Bearer " + token);
    }
}
