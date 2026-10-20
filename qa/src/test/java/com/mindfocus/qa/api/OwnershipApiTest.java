package com.mindfocus.qa.api;

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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The biggest risk of a diary: somebody else reading it. Every resource is tested the same way. */
@Tag("api")
@Epic("MindFocus")
@Feature("Authorization")
class OwnershipApiTest {

    static Stream<Arguments> resources() {
        return Stream.of(
                arguments("notes", "/api/notes", Map.of("title", "n", "content", "c", "noteDate", "2026-10-17")),
                arguments("todos", "/api/todos", Map.of("title", "t")),
                arguments("plans", "/api/plans", Map.of("title", "p", "type", "DAY", "planDate", "2026-10-17")),
                arguments("goals", "/api/goals", Map.of("title", "g", "steps", List.of(Map.of("title", "s", "weight", 1)))),
                arguments("habits", "/api/habits", Map.of("name", "h", "kind", "BUILD")));
    }

    @ParameterizedTest(name = "{0}: another user cannot read, change or delete my row")
    @MethodSource("resources")
    @Story("Users cannot reach each other's data")
    @Severity(SeverityLevel.BLOCKER)
    void other_user_gets_404_and_sees_nothing(String name, String path, Map<String, Object> body) {
        TestUser alice = Users.register();
        TestUser bob = Users.register();
        int id = alice.api().body(body).post(path).then().statusCode(201).extract().path("id");

        // 404 and not 403: Bob cannot even learn that the id exists
        bob.api().body(body).put(path + "/" + id).then().statusCode(404);
        bob.api().delete(path + "/" + id).then().statusCode(404);
        bob.api().get(path).then().statusCode(200).body("size()", is(0));

        // Alice still owns it
        alice.api().get(path).then().body("id", contains(id));
        alice.api().body(body).put(path + "/" + id).then().statusCode(200);
        alice.api().delete(path + "/" + id).then().statusCode(204);
    }

    @Test
    @Story("Mass assignment")
    void client_cannot_choose_the_id_or_the_owner_of_a_new_row() {
        TestUser alice = Users.register();
        TestUser bob = Users.register();
        int id = alice.api()
                .body(Map.of("id", 999999, "userId", 1, "title", "x", "noteDate", "2026-10-17"))
                .post("/api/notes").then().statusCode(201)
                .body("userId", nullValue())
                .extract().path("id");
        assertThat(id).isNotEqualTo(999999);
        bob.api().get("/api/notes").then().body("size()", is(0));
    }

    @Test
    @Story("Mass assignment")
    void client_cannot_preset_server_controlled_fields() {
        Users.register().api()
                .body(Map.of("title", "p", "type", "DAY", "planDate", "2026-10-17", "reminderSent", true))
                .post("/api/plans").then().statusCode(201)
                .body("reminderSent", is(false));
    }

    @Test
    @Story("Users cannot reach each other's data")
    void habit_logs_of_another_users_habit_are_protected() {
        TestUser alice = Users.register();
        TestUser bob = Users.register();
        int habit = alice.api().body(Map.of("name", "h", "kind", "QUIT")).post("/api/habits")
                .then().statusCode(201).extract().path("id");
        bob.api().body(Map.of("date", "2026-10-17", "success", true)).put("/api/habits/" + habit + "/log")
                .then().statusCode(404);
        bob.api().queryParam("date", "2026-10-17").delete("/api/habits/" + habit + "/log").then().statusCode(404);
    }
}
