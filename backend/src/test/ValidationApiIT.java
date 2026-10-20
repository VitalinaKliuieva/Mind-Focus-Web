package com.mindfocus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Negative testing: bad input must give 400 and must never reach the database or cause a 500. */
class ValidationApiIT extends AbstractIntegrationTest {

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("note without title", "/api/notes", j("{'content':'x'}")),
                Arguments.of("note with blank title", "/api/notes", j("{'title':'   '}")),
                Arguments.of("note title too long", "/api/notes", "{\"title\":\"" + "x".repeat(201) + "\"}"),
                Arguments.of("to-do without title", "/api/todos", j("{'done':false}")),
                Arguments.of("plan without planDate", "/api/plans", j("{'title':'p','type':'DAY'}")),
                Arguments.of("plan with unknown type", "/api/plans", j("{'title':'p','type':'WEEK','planDate':'2026-10-17'}")),
                Arguments.of("plan with wrong date format", "/api/plans", j("{'title':'p','type':'DAY','planDate':'17.10.2026'}")),
                Arguments.of("goal without title", "/api/goals", j("{'steps':[]}")),
                Arguments.of("[DEF-3] goal step with weight 0", "/api/goals",
                        j("{'title':'g','steps':[{'title':'s','weight':0}]}")),
                Arguments.of("[DEF-3] goal step with weight 101", "/api/goals",
                        j("{'title':'g','steps':[{'title':'s','weight':101}]}")),
                Arguments.of("[DEF-3] goal step without title", "/api/goals",
                        j("{'title':'g','steps':[{'title':'','weight':1}]}")),
                Arguments.of("habit with blank name", "/api/habits", j("{'name':'','kind':'BUILD'}")),
                Arguments.of("habit with unknown kind", "/api/habits", j("{'name':'h','kind':'MAYBE'}")),
                Arguments.of("malformed JSON", "/api/notes", "{bad"),
                Arguments.of("empty body", "/api/todos", ""));
    }

    @ParameterizedTest(name = "{0} -> 400")
    @MethodSource("invalidBodies")
    void invalidInputGives400(String name, String path, String body) throws Exception {
        String token = newUserToken();
        postJson(path, token, body).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "mood score {0} -> 400")
    @org.junit.jupiter.params.provider.ValueSource(ints = {-1, 0, 6, 100})
    void moodScoreOutOfRange(int score) throws Exception {
        putJson("/api/moods/2026-10-17", newUserToken(), "{\"score\":" + score + "}").andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("a mood on an impossible date gives 400")
    void moodWithBadDate() throws Exception {
        putJson("/api/moods/2026-13-45", newUserToken(), "{\"score\":3}").andExpect(status().isBadRequest());
        putJson("/api/moods/not-a-date", newUserToken(), "{\"score\":3}").andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("[DEF-2] a habit log without a date gives 400, not a database error")
    void habitLogWithoutDate() throws Exception {
        String token = newUserToken();
        long habit = create("/api/habits", token, j("{'name':'h','kind':'BUILD'}"));
        putJson("/api/habits/" + habit + "/log", token, j("{'success':true}")).andExpect(status().isBadRequest());
        putJson("/api/habits/" + habit + "/log", token, j("{'date':'tomorrow','success':true}")).andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("a calendar request with a bad or missing date gives 400")
    void calendarBadParameters() throws Exception {
        String token = newUserToken();
        getAs("/api/calendar?from=2026-10-01", token).andExpect(status().isBadRequest());
        getAs("/api/calendar?from=abc&to=2026-10-31", token).andExpect(status().isBadRequest());
    }
}
