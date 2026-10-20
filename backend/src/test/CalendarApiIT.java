package com.mindfocus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarApiIT extends AbstractIntegrationTest {

    private String seed() throws Exception {
        String token = newUserToken();
        create("/api/notes", token, j("{'title':'note','noteDate':'2026-10-10'}"));
        create("/api/todos", token, j("{'title':'todo','dueDate':'2026-10-12'}"));
        create("/api/goals", token, j("{'title':'goal','targetDate':'2026-10-15'}"));
        // planDate is the 1st, but the deadline is on the 20th: the calendar must use the deadline
        create("/api/plans", token, j("{'title':'plan','type':'DAY','planDate':'2026-10-01','deadline':'2026-10-20T10:00'}"));
        return token;
    }

    @Test
    @DisplayName("the feed merges notes, to-dos, goals and plans in the month")
    void mergesAllTypes() throws Exception {
        String token = seed();
        getAs("/api/calendar?from=2026-10-01&to=2026-10-31", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].type", containsInAnyOrder("note", "todo", "goal", "plan")));
    }

    @Test
    @DisplayName("events outside the range are excluded, and both range ends are inclusive")
    void rangeFiltering() throws Exception {
        String token = seed();
        getAs("/api/calendar?from=2026-10-11&to=2026-10-13", token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("todo"));
        getAs("/api/calendar?from=2026-10-12&to=2026-10-12", token)
                .andExpect(jsonPath("$.length()").value(1));
        getAs("/api/calendar?from=2026-11-01&to=2026-11-30", token)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("a plan with a deadline appears on the deadline day, not on its plan date")
    void planUsesDeadlineDay() throws Exception {
        String token = seed();
        getAs("/api/calendar?from=2026-10-01&to=2026-10-01", token).andExpect(jsonPath("$.length()").value(0));
        getAs("/api/calendar?from=2026-10-20&to=2026-10-20", token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("plan"));
    }

    @Test
    @DisplayName("another user sees none of these events")
    void calendarIsPrivate() throws Exception {
        seed();
        getAs("/api/calendar?from=2026-10-01&to=2026-10-31", newUserToken())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
