package com.mindfocus;

import com.mindfocus.config.AppProperties;
import com.mindfocus.domain.*;
import com.mindfocus.repo.*;
import com.mindfocus.service.AnalyticsService;
import com.mindfocus.service.AnalyticsService.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit test with mocked repositories: checks the dashboard numbers and the insight rules in isolation. */
class AnalyticsServiceTest {
    private static final long UID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final TodoRepository todos = mock(TodoRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final GoalRepository goals = mock(GoalRepository.class);
    private final HabitLogRepository habitLogs = mock(HabitLogRepository.class);
    private final MoodRepository moods = mock(MoodRepository.class);
    private final AppProperties props = new AppProperties("Europe/Warsaw", "", "",
            new AppProperties.Jwt("s".repeat(40), 1), new AppProperties.Push("", "", ""));
    private final AnalyticsService service = new AnalyticsService(todos, plans, goals, habitLogs, moods, props);

    @BeforeEach
    void emptyAccount() {
        when(todos.findByUserId(UID)).thenReturn(List.of());
        when(plans.findByUserId(UID)).thenReturn(List.of());
        when(goals.findByUserId(UID)).thenReturn(List.of());
        when(habitLogs.findByUserIdAndLogDateBetween(eq(UID), any(), any())).thenReturn(List.of());
        when(moods.findByUserId(UID)).thenReturn(List.of());
    }

    // ---------- helpers ----------
    private static LocalDateTime now() { return LocalDateTime.now(ZONE); }

    private static LocalDate today() { return LocalDate.now(ZONE); }

    private static TodoItem todo(boolean done, LocalDateTime completedAt) {
        TodoItem t = new TodoItem();
        t.setTitle("t");
        t.setDone(done);
        t.setCompletedAt(completedAt);
        return t;
    }

    private static Plan plan(long id, LocalDateTime deadline, boolean done) {
        Plan p = new Plan();
        p.setId(id);
        p.setTitle("plan " + id);
        p.setPlanDate(today());
        p.setDeadline(deadline);
        p.setDone(done);
        return p;
    }

    private static Goal goal(int... doneWeights) {
        Goal g = new Goal();
        g.setTitle("g");
        for (int w : doneWeights) {
            GoalStep s = new GoalStep();
            s.setTitle("s");
            s.setWeight(Math.abs(w));
            s.setDone(w > 0);
            g.getSteps().add(s);
        }
        return g;
    }

    private static MoodEntry mood(LocalDate date, int score) {
        MoodEntry m = new MoodEntry();
        m.setEntryDate(date);
        m.setScore(score);
        return m;
    }

    private static HabitLog log(boolean success) {
        HabitLog l = new HabitLog();
        l.setHabitId(1L);
        l.setLogDate(today());
        l.setSuccess(success);
        return l;
    }

    // ---------- tests ----------
    @Test
    @DisplayName("an empty account has zero totals and a nudge to start")
    void emptyAccountHasZeroTotals() {
        Summary s = service.summary(UID, 14);
        assertThat(s.totals().todosDone()).isZero();
        assertThat(s.totals().todosOpen()).isZero();
        assertThat(s.totals().plansOverdue()).isZero();
        assertThat(s.totals().goalsAverageProgress()).isZero();
        assertThat(s.totals().moodAverage()).isNull();
        assertThat(s.upcoming()).isEmpty();
        assertThat(s.insights()).anyMatch(i -> i.contains("Nothing ticked off"));
    }

    @Test
    @DisplayName("the chart has exactly one point per requested day, ending today")
    void seriesLengthFollowsDays() {
        assertThat(service.summary(UID, 1).series()).hasSize(1);
        assertThat(service.summary(UID, 7).series()).hasSize(7);
        Summary s = service.summary(UID, 14);
        assertThat(s.series()).hasSize(14);
        assertThat(s.series().get(13).date()).isEqualTo(today());
        assertThat(s.series().get(0).date()).isEqualTo(today().minusDays(13));
    }

    @Test
    @DisplayName("a to-do finished today counts in today's bar and changes the insight")
    void todoFinishedTodayIsCounted() {
        when(todos.findByUserId(UID)).thenReturn(List.of(todo(true, now()), todo(false, null)));
        Summary s = service.summary(UID, 14);
        assertThat(s.totals().todosDone()).isEqualTo(1);
        assertThat(s.totals().todosOpen()).isEqualTo(1);
        assertThat(s.series().get(13).todosCompleted()).isEqualTo(1);
        assertThat(s.insights()).contains("You have finished 1 thing today.");
        assertThat(s.insights()).noneMatch(i -> i.contains("Nothing ticked off"));
    }

    @Test
    @DisplayName("a to-do finished yesterday is not counted as today's work")
    void todoFinishedYesterdayIsNotToday() {
        when(todos.findByUserId(UID)).thenReturn(List.of(todo(true, now().minusDays(1))));
        Summary s = service.summary(UID, 14);
        assertThat(s.series().get(13).todosCompleted()).isZero();
        assertThat(s.series().get(12).todosCompleted()).isEqualTo(1);
        assertThat(s.insights()).anyMatch(i -> i.contains("Nothing ticked off"));
    }

    @Test
    @DisplayName("overdue means: not done and the deadline is in the past")
    void overdueRule() {
        when(plans.findByUserId(UID)).thenReturn(List.of(
                plan(1, now().minusHours(1), false),   // overdue
                plan(2, now().minusHours(1), true),    // late but done: not overdue
                plan(3, now().plusHours(1), false),    // future: not overdue
                plan(4, null, false)));                // no deadline: never overdue
        Summary s = service.summary(UID, 14);
        assertThat(s.totals().plansOverdue()).isEqualTo(1);
        assertThat(s.insights()).anyMatch(i -> i.contains("1 plan is past the deadline"));
    }

    @Test
    @DisplayName("upcoming deadlines are sorted, limited to five, and exclude done or past ones")
    void upcomingIsSortedAndLimited() {
        List<Plan> list = new ArrayList<>();
        for (int i = 7; i >= 1; i--) list.add(plan(i, now().plusHours(i * 3L), false));
        list.add(plan(100, now().minusHours(2), false));   // past
        list.add(plan(101, now().plusHours(1), true));     // done
        when(plans.findByUserId(UID)).thenReturn(list);
        Summary s = service.summary(UID, 14);
        assertThat(s.upcoming()).hasSize(5);
        assertThat(s.upcoming()).extracting(AnalyticsService.Upcoming::id).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(s.insights()).anyMatch(i -> i.contains("due in the next 48 hours"));
    }

    @Test
    @DisplayName("average goal progress uses each goal's weighted progress")
    void goalsAverage() {
        when(goals.findByUserId(UID)).thenReturn(List.of(goal(1, -1), goal(2)));   // 50% and 100%
        assertThat(service.summary(UID, 14).totals().goalsAverageProgress()).isEqualTo(75);
    }

    @Test
    @DisplayName("habit success rate is successes divided by logs in the window")
    void habitRate() {
        when(habitLogs.findByUserIdAndLogDateBetween(eq(UID), any(), any()))
                .thenReturn(List.of(log(true), log(true), log(true), log(false)));
        Summary s = service.summary(UID, 14);
        assertThat(s.totals().habitSuccessRate()).isEqualTo(75);
        assertThat(s.insights()).anyMatch(i -> i.contains("75%"));
    }

    @Test
    @DisplayName("mood average only uses entries inside the window and drives the insight")
    void moodAverageAndInsight() {
        when(moods.findByUserId(UID)).thenReturn(List.of(
                mood(today(), 5), mood(today().minusDays(1), 4), mood(today().minusDays(30), 1)));
        Summary s = service.summary(UID, 14);
        assertThat(s.totals().moodAverage()).isEqualTo(4.5);
        assertThat(s.series().get(13).mood()).isEqualTo(5);
        assertThat(s.insights()).anyMatch(i -> i.contains("bright"));
    }

    @Test
    @DisplayName("a low mood average produces the gentle warning")
    void lowMood() {
        when(moods.findByUserId(UID)).thenReturn(List.of(mood(today(), 1), mood(today().minusDays(1), 2)));
        assertThat(service.summary(UID, 14).insights()).anyMatch(i -> i.contains("low lately"));
    }
}
