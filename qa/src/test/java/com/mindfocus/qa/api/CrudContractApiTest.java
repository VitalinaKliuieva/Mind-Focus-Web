package com.mindfocus.qa.api;

import com.mindfocus.qa.support.TestUser;
import com.mindfocus.qa.support.Users;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag("api")
@Epic("MindFocus")
class CrudContractApiTest {

    record StepSpec(int weight, boolean done) {}

    // ---------------- notes ----------------

    @Test
    @Tag("smoke")
    @Feature("Notes")
    @Story("Full lifecycle and response contract")
    void note_create_read_update_delete() {
        TestUser user = Users.register();
        Response created = user.api().body(Map.of("title", "First", "content", "Hello", "noteDate", "2026-10-17")).post("/api/notes");
        created.then().statusCode(201);
        Map<String, Object> note = created.as(new TypeRef<Map<String, Object>>() {});

        // contract: required fields and types; the owner id is never exposed
        assertThat(note).containsKeys("id", "title", "content", "noteDate", "createdAt").doesNotContainKey("userId");
        assertThat(note.get("id")).isInstanceOf(Integer.class);
        assertThat(note.get("title")).isInstanceOf(String.class);

        Map<String, Object> changed = new HashMap<>(note);
        changed.put("title", "Changed");
        user.api().body(changed).put("/api/notes/" + note.get("id")).then().statusCode(200).body("title", is("Changed"));
        user.api().get("/api/notes").then().body("[0].title", is("Changed"));

        user.api().delete("/api/notes/" + note.get("id")).then().statusCode(204);
        user.api().get("/api/notes").then().body("size()", is(0));
    }

    @Test
    @Feature("Notes")
    @Story("Ordering")
    void notes_are_listed_newest_date_first() {
        TestUser user = Users.register();
        for (String day : List.of("2026-10-10", "2026-10-12", "2026-10-11")) {
            user.api().body(Map.of("title", day, "noteDate", day)).post("/api/notes").then().statusCode(201);
        }
        user.api().get("/api/notes").then().body("noteDate", contains("2026-10-12", "2026-10-11", "2026-10-10"));
    }

    @Test
    @Feature("Notes")
    @Story("Not found")
    void unknown_id_gives_404() {
        TestUser user = Users.register();
        user.api().body(Map.of("title", "x")).put("/api/notes/99999999").then().statusCode(404);
        user.api().delete("/api/notes/99999999").then().statusCode(404);
    }

    // ---------------- to-do ----------------

    @Test
    @Feature("To-do")
    @Story("Completion time")
    void completed_at_is_set_when_ticked_and_cleared_when_reopened() {
        TestUser user = Users.register();
        int id = user.api().body(Map.of("title", "write tests")).post("/api/todos").then().statusCode(201)
                .body("done", is(false)).body("completedAt", nullValue()).extract().path("id");

        user.api().body(Map.of("title", "write tests", "done", true)).put("/api/todos/" + id)
                .then().body("done", is(true)).body("completedAt", notNullValue());
        user.api().body(Map.of("title", "write tests", "done", false)).put("/api/todos/" + id)
                .then().body("completedAt", nullValue());
    }

    // ---------------- plans ----------------

    @Test
    @Feature("Planner")
    @Story("Defaults")
    void a_new_plan_is_not_reminded_and_defaults_to_one_hour_before() {
        Users.register().api()
                .body(Map.of("title", "p", "type", "DAY", "planDate", "2026-10-17", "deadline", "2099-01-01T10:00"))
                .post("/api/plans").then().statusCode(201)
                .body("reminderSent", is(false))
                .body("reminderMinutesBefore", is(60));
    }

    static Stream<Arguments> invalidPlans() {
        return Stream.of(
                arguments("no date", Map.of("title", "p", "type", "DAY")),
                arguments("unknown type", Map.of("title", "p", "type", "WEEK", "planDate", "2026-10-17")),
                arguments("bad date format", Map.of("title", "p", "type", "DAY", "planDate", "17.10.2026")),
                arguments("no title", Map.of("type", "DAY", "planDate", "2026-10-17")));
    }

    @ParameterizedTest(name = "plan with {0} -> 400")
    @MethodSource("invalidPlans")
    @Feature("Planner")
    @Story("Validation")
    void invalid_plan_is_rejected(String label, Map<String, Object> body) {
        Users.register().api().body(body).post("/api/plans").then().statusCode(400);
    }

    // ---------------- goals ----------------

    static Stream<Arguments> weightedProgress() {
        return Stream.of(
                arguments("no steps", List.of(), 0),
                arguments("nothing done", List.of(new StepSpec(1, false), new StepSpec(3, false)), 0),
                arguments("small step done", List.of(new StepSpec(1, true), new StepSpec(3, false)), 25),
                arguments("big step done", List.of(new StepSpec(1, false), new StepSpec(3, true)), 75),
                arguments("everything done", List.of(new StepSpec(1, true), new StepSpec(3, true)), 100),
                arguments("rounds down", List.of(new StepSpec(1, true), new StepSpec(2, false)), 33),
                arguments("rounds up", List.of(new StepSpec(1, false), new StepSpec(2, true)), 67));
    }

    @ParameterizedTest(name = "{0}: {2}%")
    @MethodSource("weightedProgress")
    @Feature("Goals")
    @Story("Weighted progress")
    void progress_follows_the_step_weights(String label, List<StepSpec> steps, int expected) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            payload.add(Map.of("title", "step " + i, "weight", steps.get(i).weight(), "done", steps.get(i).done()));
        }
        Users.register().api().body(Map.of("title", "goal", "steps", payload)).post("/api/goals")
                .then().statusCode(201).body("progress", is(expected));
    }

    static Stream<Arguments> invalidSteps() {
        return Stream.of(
                arguments("weight 0", Map.of("title", "s", "weight", 0)),
                arguments("weight 101", Map.of("title", "s", "weight", 101)),
                arguments("blank title", Map.of("title", "", "weight", 1)));
    }

    @ParameterizedTest(name = "[DEF-3] step with {0} -> 400")
    @MethodSource("invalidSteps")
    @Feature("Goals")
    @Story("Validation")
    @Issue("DEF-3")
    void invalid_step_is_rejected(String label, Map<String, Object> step) {
        Users.register().api().body(Map.of("title", "g", "steps", List.of(step))).post("/api/goals").then().statusCode(400);
    }
}
