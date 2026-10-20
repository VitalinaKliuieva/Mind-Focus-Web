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
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@Tag("api")
@Epic("MindFocus")
class AnalyticsApiTest {

    @Test
    @Tag("smoke")
    @Feature("Analytics")
    @Story("Adapts after every action")
    @Severity(SeverityLevel.CRITICAL)
    void summary_changes_after_add_tick_and_untick() {
        TestUser user = Users.register();
        user.api().get("/api/analytics/summary").then().body("totals.todosOpen", is(0));

        int id = user.api().body(Map.of("title", "write tests")).post("/api/todos").then().statusCode(201).extract().path("id");
        user.api().get("/api/analytics/summary").then()
                .body("totals.todosOpen", is(1)).body("totals.todosDone", is(0));

        user.api().body(Map.of("title", "write tests", "done", true)).put("/api/todos/" + id).then().statusCode(200);
        user.api().get("/api/analytics/summary").then()
                .body("totals.todosOpen", is(0)).body("totals.todosDone", is(1))
                .body("series[-1].todosCompleted", is(1))
                .body("insights", hasItem("You have finished 1 thing today."));

        user.api().body(Map.of("title", "write tests", "done", false)).put("/api/todos/" + id).then().statusCode(200);
        user.api().get("/api/analytics/summary").then()
                .body("totals.todosDone", is(0)).body("series[-1].todosCompleted", is(0));
    }

    @ParameterizedTest(name = "days={0} gives {1} points")
    @CsvSource({"0,1", "-5,1", "1,1", "7,7", "90,90", "999,90"})
    @Feature("Analytics")
    @Story("Parameters")
    void days_parameter_is_clamped_between_1_and_90(int days, int length) {
        Users.register().api().queryParam("days", days).get("/api/analytics/summary")
                .then().statusCode(200).body("series.size()", is(length));
    }

    @Test
    @Feature("Analytics")
    @Story("Parameters")
    void a_non_numeric_days_value_gives_400() {
        Users.register().api().queryParam("days", "abc").get("/api/analytics/summary").then().statusCode(400);
    }

    @Test
    @Feature("Calendar")
    void a_plan_appears_on_its_deadline_day_and_not_on_its_plan_date() {
        TestUser user = Users.register();
        user.api().body(Map.of("title", "p", "type", "DAY", "planDate", "2026-10-01", "deadline", "2026-10-20T10:00"))
                .post("/api/plans").then().statusCode(201);

        user.api().queryParam("from", "2026-10-01").queryParam("to", "2026-10-01").get("/api/calendar")
                .then().body("size()", is(0));
        user.api().queryParam("from", "2026-10-20").queryParam("to", "2026-10-20").get("/api/calendar")
                .then().body("type", contains("plan"));
    }

    @Test
    @Feature("Calendar")
    void both_ends_of_the_range_are_inclusive() {
        TestUser user = Users.register();
        user.api().body(Map.of("title", "t", "dueDate", "2026-10-12")).post("/api/todos").then().statusCode(201);
        user.api().queryParam("from", "2026-10-12").queryParam("to", "2026-10-12").get("/api/calendar")
                .then().body("size()", is(1));
        user.api().queryParam("from", "2026-10-13").queryParam("to", "2026-10-20").get("/api/calendar")
                .then().body("size()", is(0));
    }
}
