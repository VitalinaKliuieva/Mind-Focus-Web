package com.mindfocus.qa.support;

import io.restassured.specification.RequestSpecification;

import java.util.Map;

/** A freshly registered user. Each test creates its own, so tests never share data. */
public record TestUser(String email, String password, String token, Map<String, Object> profile) {

    public RequestSpecification api() {
        return Api.as(token);
    }
}
