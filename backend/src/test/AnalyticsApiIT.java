package com.mindfocus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks the "adapts after every action" requirement end to end. */
class AnalyticsApiIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("the summary changes after each action: add, tick, untick")
    void summaryAdaptsAfterEveryAction() throws Exception {
        String token = newUserToken();

        getAs("/api/analytics/summary", token)
                .andExpect(jsonPath("$.totals.todosOpen").value(0))
                .andExpect(jsonPath("$.insights", hasItem(containsString("Nothing ticked off"))));

        long id = create("/api/todos", token, j("{'title':'write tests'}"));
        getAs("/api/analytics/summary", token)
                .andExpect(jsonPath("$.totals.todosOpen").value(1))
                .andExpect(jsonPath("$.totals.todosDone").value(0));

        putJson("/api/todos/" + id, token, j("{'title':'write tests','done':true}")).andExpect(status().isOk());
        getAs("/api/analytics/summary", token)
                .andExpect(jsonPath("$.totals.todosOpen").value(0))
                .andExpect(jsonPath("$.totals.todosDone").value(1))
                .andExpect(jsonPath("$.series[13].todosCompleted").value(1))
                .andExpect(jsonPath("$.insights", hasItem("You have finished 1 thing today.")));

        putJson("/api/todos/" + id, token, j("{'title':'write tests','done':false}")).andExpect(status().isOk());
        getAs("/api/analytics/summary", token)
                .andExpect(jsonPath("$.totals.todosDone").value(0))
                .andExpect(jsonPath("$.series[13].todosCompleted").value(0))
                .andExpect(jsonPath("$.insights", hasItem(containsString("Nothing ticked off"))));
    }

    @Test
    @DisplayName("the days parameter is clamped between 1 and 90")
    void daysParameterIsClamped() throws Exception {
        String token = newUserToken();
        getAs("/api/analytics/summary?days=0", token).andExpect(jsonPath("$.series.length()").value(1));
        getAs("/api/analytics/summary?days=-5", token).andExpect(jsonPath("$.series.length()").value(1));
        getAs("/api/analytics/summary?days=7", token).andExpect(jsonPath("$.series.length()").value(7));
        getAs("/api/analytics/summary?days=999", token).andExpect(jsonPath("$.series.length()").value(90));
    }

    @Test
    @DisplayName("a non-numeric days parameter gives 400")
    void daysMustBeANumber() throws Exception {
        getAs("/api/analytics/summary?days=abc", newUserToken()).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("goal progress and mood feed the totals")
    void goalsAndMoodFeedTotals() throws Exception {
        String token = newUserToken();
        create("/api/goals", token, j("{'title':'g','steps':[{'title':'a','weight':1,'done':true},{'title':'b','weight':1,'done':false}]}"));
        putJson("/api/moods/" + java.time.LocalDate.now(), token, j("{'score':4}")).andExpect(status().isOk());
        getAs("/api/analytics/summary", token)
                .andExpect(jsonPath("$.totals.goalsAverageProgress").value(50))
                .andExpect(jsonPath("$.totals.moodAverage").value(4.0));
    }
}
