package com.mindfocus.qa.api;

import com.mindfocus.qa.support.Dates;
import com.mindfocus.qa.support.TestUser;
import com.mindfocus.qa.support.Users;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag("api")
@Epic("MindFocus")
@Feature("Habits")
class HabitApiTest {
    private TestUser user;
    private int habit;

    @BeforeEach
    void createHabit() {
        user = Users.register();
        habit = user.api().body(Map.of("name", "No sugar", "kind", "QUIT")).post("/api/habits")
                .then().statusCode(201).extract().path("id");
    }

    private void log(int daysAgo, boolean success) {
        user.api().body(Map.of("date", Dates.daysAgo(daysAgo), "success", success))
                .put("/api/habits/" + habit + "/log").then().statusCode(200);
    }

    private Map<String, Object> stat() {
        return user.api().get("/api/habits/stats").then().statusCode(200).extract().jsonPath().getMap("[0]");
    }

    private int number(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }

    @Test
    @Story("Daily log")
    void logging_the_same_day_twice_keeps_one_row_and_the_last_value_wins() {
        log(0, true);
        log(0, false);
        user.api().queryParam("from", Dates.daysAgo(0)).queryParam("to", Dates.daysAgo(0)).get("/api/habits/logs")
                .then().body("size()", is(1)).body("[0].success", is(false));
    }

    static Stream<Arguments> streaks() {
        return Stream.of(
                //          scenario,                               successes ago,         slips ago,    current, best
                arguments("no logs",                                new int[]{},             new int[]{},  0, 0),
                arguments("three in a row ending today",            new int[]{0, 1, 2},      new int[]{},  3, 3),
                arguments("today not logged yet keeps yesterday",   new int[]{1, 2},         new int[]{},  2, 2),
                arguments("a gap ends the current streak",          new int[]{0, 1, 3, 4, 5}, new int[]{}, 2, 3),
                arguments("old streak that already ended",         new int[]{2, 3},         new int[]{},  0, 2),
                arguments("[DEF-1] a slip today resets it",         new int[]{1, 2},         new int[]{0}, 0, 2));
    }

    @ParameterizedTest(name = "{0}: current {3}, best {4}")
    @MethodSource("streaks")
    @Story("Streaks")
    void streak_calculation(String scenario, int[] successes, int[] slips, int current, int best) {
        for (int d : successes) log(d, true);
        for (int d : slips) log(d, false);
        Map<String, Object> stat = stat();
        assertThat(number(stat, "currentStreak")).as("current streak").isEqualTo(current);
        assertThat(number(stat, "bestStreak")).as("best streak").isEqualTo(best);
    }

    @Test
    @Story("Success rate")
    void thirty_day_rate_ignores_older_logs() {
        log(0, true);
        log(1, true);
        log(2, true);
        log(3, false);
        log(40, true);
        assertThat(number(stat(), "successRate30")).isEqualTo(75);
    }

    static Stream<Arguments> badLogBodies() {
        Map<String, Object> nullDate = new HashMap<>();
        nullDate.put("date", null);
        nullDate.put("success", true);
        return Stream.of(
                arguments("missing date", Map.of("success", true)),
                arguments("null date", nullDate),
                arguments("text instead of a date", Map.of("date", "tomorrow", "success", true)));
    }

    @ParameterizedTest(name = "[DEF-2] {0} -> 400")
    @MethodSource("badLogBodies")
    @Story("Validation")
    @Issue("DEF-2")
    void log_with_a_bad_date_is_rejected(String label, Map<String, Object> body) {
        user.api().body(body).put("/api/habits/" + habit + "/log").then().statusCode(400);
    }

    @Test
    @Story("Cleanup")
    void deleting_a_habit_deletes_its_logs() {
        log(0, true);
        user.api().delete("/api/habits/" + habit).then().statusCode(204);
        user.api().queryParam("from", Dates.daysAgo(5)).queryParam("to", Dates.daysAgo(0)).get("/api/habits/logs")
                .then().body("size()", is(0));
    }
}
