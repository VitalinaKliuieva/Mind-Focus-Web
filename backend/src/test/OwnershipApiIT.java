package com.mindfocus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization tests. The most important risk of this app is one user reaching another user's diary
 * (insecure direct object reference), so every resource is tested the same way.
 */
class OwnershipApiIT extends AbstractIntegrationTest {

    static Stream<Arguments> resources() {
        return Stream.of(
                Arguments.of("/api/notes", j("{'title':'n','content':'c','noteDate':'2026-10-17'}")),
                Arguments.of("/api/todos", j("{'title':'t'}")),
                Arguments.of("/api/plans", j("{'title':'p','type':'DAY','planDate':'2026-10-17'}")),
                Arguments.of("/api/goals", j("{'title':'g','steps':[{'title':'s','weight':1}]}")),
                Arguments.of("/api/habits", j("{'name':'h','kind':'BUILD'}")));
    }

    @ParameterizedTest(name = "{0}: user B cannot read, change or delete user A's row")
    @MethodSource("resources")
    void otherUsersCannotTouchMyData(String path, String body) throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        long id = create(path, alice, body);

        // Bob gets 404 (not 403), so he cannot even learn that the id exists
        putJson(path + "/" + id, bob, body).andExpect(status().isNotFound());
        deleteAs(path + "/" + id, bob).andExpect(status().isNotFound());
        getAs(path, bob).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // Alice still has her row and can still use it
        getAs(path, alice).andExpect(jsonPath("$[0].id").value(id));
        putJson(path + "/" + id, alice, body).andExpect(status().isOk());
        deleteAs(path + "/" + id, alice).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("a client cannot choose the id or the owner of a new row (mass assignment)")
    void clientCannotChooseIdOrOwner() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        long id = create("/api/notes", alice, j("{'id':999999,'userId':1,'title':'x','noteDate':'2026-10-17'}"));
        assertNotEquals(999999L, id);
        getAs("/api/notes", bob).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("a client cannot pre-set server-controlled fields such as reminderSent")
    void clientCannotSetReminderSent() throws Exception {
        String alice = newUserToken();
        postJson("/api/plans", alice, j("{'title':'p','type':'DAY','planDate':'2026-10-17','reminderSent':true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderSent").value(false));
    }

    @Test
    @DisplayName("habit logs of another user's habit cannot be written or cleared")
    void habitLogsAreProtected() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        long habit = create("/api/habits", alice, j("{'name':'h','kind':'QUIT'}"));
        putJson("/api/habits/" + habit + "/log", bob, j("{'date':'2026-10-17','success':true}"))
                .andExpect(status().isNotFound());
        deleteAs("/api/habits/" + habit + "/log?date=2026-10-17", bob).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("moods and analytics are per user")
    void moodsAndAnalyticsArePerUser() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        putJson("/api/moods/2026-10-17", alice, j("{'score':5}")).andExpect(status().isOk());
        getAs("/api/moods", bob).andExpect(jsonPath("$.length()").value(0));
        getAs("/api/analytics/summary", bob).andExpect(jsonPath("$.totals.moodAverage").doesNotExist());
    }
}
