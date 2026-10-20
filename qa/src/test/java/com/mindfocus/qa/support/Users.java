package com.mindfocus.qa.support;

import io.restassured.response.Response;

import java.util.Map;
import java.util.UUID;

public final class Users {
    private Users() {}

    public static String uniqueEmail() {
        return "qa-" + UUID.randomUUID() + "@example.com";
    }

    public static TestUser register() {
        return register("QA Tester");
    }

    public static TestUser register(String displayName) {
        String email = uniqueEmail();
        Response response = Api.anon()
                .body(Map.of("email", email, "password", Settings.PASSWORD, "displayName", displayName))
                .post("/api/auth/register");
        response.then().statusCode(201);
        return new TestUser(email, Settings.PASSWORD, response.path("token"), response.path("user"));
    }
}
