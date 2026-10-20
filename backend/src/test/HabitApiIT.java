package com.mindfocus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HabitApiIT extends AbstractIntegrationTest {
    private String token;
    private long habitId;

    @BeforeEach
    void setUp() throws Exception {
        token = newUserToken();
        habitId = create("/api/habits", token, j("{'name':'No sugar','kind':'QUIT'}"));
    }

    private static LocalDate daysAgo(int n) {
        return LocalDate.now().minusDays(n);
    }

    private void log(LocalDate date, boolean success) throws Exception {
        putJson("/api/habits/" + habitId + "/log", token, j("{'date':'" + date + "','success':" + success + "}"))
                .andExpect(status().isOk());
    }

    private JsonNode stat() throws Exception {
        return read(getAs("/api/habits/stats", token)).get(0);
    }

    @Test
    @DisplayName("logging the same day twice keeps a single row and the last value wins")
    void sameDayIsAnUpsert() throws Exception {
        log(daysAgo(0), true);
        log(daysAgo(0), false);
        getAs("/api/habits/logs?from=" + daysAgo(0) + "&to=" + daysAgo(0), token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].success").value(false));
    }

    @Test
    @DisplayName("clearing a day removes the log")
    void clearDay() throws Exception {
        log(daysAgo(0), true);
        deleteAs("/api/habits/" + habitId + "/log?date=" + daysAgo(0), token).andExpect(status().isOk());
        getAs("/api/habits/logs?from=" + daysAgo(1) + "&to=" + daysAgo(0), token)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("the log range is inclusive on both ends")
    void rangeIsInclusive() throws Exception {
        log(daysAgo(2), true);
        log(daysAgo(0), true);
        getAs("/api/habits/logs?from=" + daysAgo(2) + "&to=" + daysAgo(0), token)
                .andExpect(jsonPath("$.length()").value(2));
        getAs("/api/habits/logs?from=" + daysAgo(1) + "&to=" + daysAgo(1), token)
                .andExpect(jsonPath("$.length()").value(0));
    }

    static Stream<Arguments> streakCases() {
        return Stream.of(
                //          name,                                   success days ago, slip days ago, current, best
                Arguments.of("no logs at all",                       new int[]{},              new int[]{},  0, 0),
                Arguments.of("three in a row ending today",          new int[]{0, 1, 2},       new int[]{},  3, 3),
                Arguments.of("today not logged yet keeps yesterday", new int[]{1, 2},          new int[]{},  2, 2),
                Arguments.of("a gap ends the current streak",        new int[]{0, 1, 3, 4, 5}, new int[]{},  2, 3),
                Arguments.of("old streak that already ended",        new int[]{2, 3},          new int[]{},  0, 2),
                Arguments.of("[DEF-1] a slip today resets it",       new int[]{1, 2},          new int[]{0}, 0, 2),
                Arguments.of("a slip yesterday ends it, today counts", new int[]{0, 2, 3},     new int[]{1}, 1, 2));
    }

    @ParameterizedTest(name = "{0}: current {3}, best {4}")
    @MethodSource("streakCases")
    void streakCalculation(String name, int[] success, int[] slips, int current, int best) throws Exception {
        for (int d : success) log(daysAgo(d), true);
        for (int d : slips) log(daysAgo(d), false);
        JsonNode s = stat();
        assertThat(s.get("currentStreak").asInt()).as("current streak").isEqualTo(current);
        assertThat(s.get("bestStreak").asInt()).as("best streak").isEqualTo(best);
    }

    @Test
    @DisplayName("30-day success rate is successes divided by logged days")
    void successRate() throws Exception {
        log(daysAgo(0), true);
        log(daysAgo(1), true);
        log(daysAgo(2), true);
        log(daysAgo(3), false);
        log(daysAgo(40), true);   // outside the 30-day window
        assertThat(stat().get("successRate30").asInt()).isEqualTo(75);
    }

    @Test
    @DisplayName("deleting a habit also deletes its logs")
    void deletingHabitDeletesLogs() throws Exception {
        log(daysAgo(0), true);
        deleteAs("/api/habits/" + habitId, token).andExpect(status().isNoContent());
        getAs("/api/habits/logs?from=" + daysAgo(5) + "&to=" + daysAgo(0), token)
                .andExpect(jsonPath("$.length()").value(0));
        getAs("/api/habits", token).andExpect(jsonPath("$.length()").value(0));
    }
}
