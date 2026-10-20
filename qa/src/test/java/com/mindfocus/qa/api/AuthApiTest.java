package com.mindfocus.qa.api;

import com.mindfocus.qa.support.Api;
import com.mindfocus.qa.support.Settings;
import com.mindfocus.qa.support.TestUser;
import com.mindfocus.qa.support.Users;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag("api")
@Epic("MindFocus")
@Feature("Authentication")
class AuthApiTest {

    private static Map<String, Object> registration(String email, String password, String name) {
        return Map.of("email", email, "password", password, "displayName", name);
    }

    // ---------------- registration ----------------

    @Test
    @Tag("smoke")
    @Story("Register")
    @Severity(SeverityLevel.BLOCKER)
    void register_returns_token_and_user_but_never_the_password() {
        String email = Users.uniqueEmail();
        Api.anon().body(registration(email, Settings.PASSWORD, "Ann"))
                .post("/api/auth/register")
                .then().statusCode(201)
                .body("token", not(emptyString()))
                .body("user.email", equalTo(email))
                .body("user", not(hasKey("passwordHash")))
                .body("user", not(hasKey("password")));
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                arguments("bad email", "not-an-email", Settings.PASSWORD, "Ann"),
                arguments("password with 7 characters", "a@b.pl", "1234567", "Ann"),
                arguments("password with 101 characters", "a@b.pl", "p".repeat(101), "Ann"),
                arguments("blank name", "a@b.pl", Settings.PASSWORD, ""),
                arguments("whitespace name", "a@b.pl", Settings.PASSWORD, "   "));
    }

    @ParameterizedTest(name = "{0} is rejected with 400")
    @MethodSource("invalidRegistrations")
    @Story("Register")
    @Severity(SeverityLevel.CRITICAL)
    void invalid_registration_is_rejected(String label, String email, String password, String name) {
        Api.anon().body(registration(email, password, name)).post("/api/auth/register").then().statusCode(400);
    }

    @Test
    @Story("Register")
    void boundary_password_of_8_characters_is_accepted() {
        Api.anon().body(registration(Users.uniqueEmail(), "12345678", "Ann")).post("/api/auth/register").then().statusCode(201);
    }

    @Test
    @Story("Register")
    @Severity(SeverityLevel.CRITICAL)
    void duplicate_email_is_rejected_even_in_another_letter_case() {
        TestUser user = Users.register();
        Api.anon().body(registration(user.email().toUpperCase(), Settings.PASSWORD, "Copy"))
                .post("/api/auth/register").then().statusCode(409);
    }

    // ---------------- login ----------------

    @Test
    @Tag("smoke")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    void login_works_with_any_letter_case_and_the_token_opens_me() {
        TestUser user = Users.register();
        String token = Api.anon().body(Map.of("email", user.email().toUpperCase(), "password", Settings.PASSWORD))
                .post("/api/auth/login").then().statusCode(200).extract().path("token");
        Api.as(token).get("/api/auth/me").then().statusCode(200).body("email", equalTo(user.email()));
    }

    @Test
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    void wrong_password_and_unknown_email_look_identical_so_emails_cannot_be_guessed() {
        TestUser user = Users.register();
        String wrongPassword = Api.anon().body(Map.of("email", user.email(), "password", "wrong-password"))
                .post("/api/auth/login").then().statusCode(401).extract().path("message");
        String unknownEmail = Api.anon().body(Map.of("email", Users.uniqueEmail(), "password", Settings.PASSWORD))
                .post("/api/auth/login").then().statusCode(401).extract().path("message");
        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    // ---------------- access control ----------------

    static Stream<String> protectedPaths() {
        return Stream.of("/api/notes", "/api/plans", "/api/todos", "/api/goals", "/api/habits", "/api/habits/stats",
                "/api/moods", "/api/calendar?from=2026-10-01&to=2026-10-31", "/api/analytics/summary",
                "/api/notifications", "/api/auth/me");
    }

    @ParameterizedTest(name = "{0} needs a login")
    @MethodSource("protectedPaths")
    @Story("Access control")
    @Severity(SeverityLevel.BLOCKER)
    void protected_endpoints_return_401_without_a_token(String path) {
        Api.anon().get(path).then().statusCode(401);
    }

    @ParameterizedTest(name = "token \"{0}\" is rejected")
    @ValueSource(strings = {"garbage", "Bearer", "a.b.c", "x"})
    @Story("Access control")
    void malformed_tokens_are_rejected(String token) {
        Api.as(token).get("/api/notes").then().statusCode(401);
    }
}
