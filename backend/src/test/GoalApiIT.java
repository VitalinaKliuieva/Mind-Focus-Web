package com.mindfocus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalApiIT extends AbstractIntegrationTest {

    private static final String TWO_STEPS_NONE_DONE =
            "{'title':'Run 10k','steps':[{'title':'a','weight':1,'done':false},{'title':'b','weight':3,'done':false}]}";

    @Test
    @DisplayName("a new goal starts at 0% and keeps its steps in order")
    void newGoalStartsAtZero() throws Exception {
        String token = newUserToken();
        postJson("/api/goals", token, j(TWO_STEPS_NONE_DONE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].title").value("a"))
                .andExpect(jsonPath("$.steps[1].title").value("b"));
    }

    @Test
    @DisplayName("ticking the small step gives 25%, ticking both gives 100%")
    void progressFollowsTheWeights() throws Exception {
        String token = newUserToken();
        long id = create("/api/goals", token, j(TWO_STEPS_NONE_DONE));

        putJson("/api/goals/" + id, token,
                j("{'title':'Run 10k','steps':[{'title':'a','weight':1,'done':true},{'title':'b','weight':3,'done':false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(25));

        putJson("/api/goals/" + id, token,
                j("{'title':'Run 10k','steps':[{'title':'a','weight':1,'done':true},{'title':'b','weight':3,'done':true}]}"))
                .andExpect(jsonPath("$.progress").value(100));
    }

    @Test
    @DisplayName("updating replaces the steps: removed steps are gone, new ones appear")
    void updateReplacesSteps() throws Exception {
        String token = newUserToken();
        long id = create("/api/goals", token, j(TWO_STEPS_NONE_DONE));

        putJson("/api/goals/" + id, token, j("{'title':'Run 10k','steps':[{'title':'only','weight':2,'done':true}]}"))
                .andExpect(jsonPath("$.steps.length()").value(1))
                .andExpect(jsonPath("$.steps[0].title").value("only"))
                .andExpect(jsonPath("$.progress").value(100));

        getAs("/api/goals", token).andExpect(jsonPath("$[0].steps.length()").value(1));
    }

    @Test
    @DisplayName("a goal without steps is allowed and shows 0%")
    void goalWithoutSteps() throws Exception {
        postJson("/api/goals", newUserToken(), j("{'title':'Someday'}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    @DisplayName("deleting a goal deletes it from the list")
    void deleteGoal() throws Exception {
        String token = newUserToken();
        long id = create("/api/goals", token, j(TWO_STEPS_NONE_DONE));
        deleteAs("/api/goals/" + id, token).andExpect(status().isNoContent());
        getAs("/api/goals", token).andExpect(jsonPath("$.length()").value(0));
        assertThat(id).isPositive();
    }
}
