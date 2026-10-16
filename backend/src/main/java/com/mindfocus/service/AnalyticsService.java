package com.mindfocus.service;

import com.mindfocus.config.AppProperties;
import com.mindfocus.domain.*;
import com.mindfocus.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the dashboard numbers. The frontend re-requests this after every user action. */
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TodoRepository todos;
    private final PlanRepository plans;
    private final GoalRepository goals;
    private final HabitLogRepository habitLogs;
    private final MoodRepository moods;
    private final AppProperties props;

    public record Totals(int todosDone, int todosOpen, int plansDone, int plansOpen, int plansOverdue,
                         int goalsAverageProgress, int habitSuccessRate, Double moodAverage) {}

    public record DayPoint(LocalDate date, int todosCompleted, int plansCompleted, int habitsDone, Integer mood) {}

    public record Upcoming(Long id, String title, LocalDateTime deadline) {}

    public record Summary(Totals totals, List<DayPoint> series, List<String> insights, List<Upcoming> upcoming) {}

    @Transactional(readOnly = true)
    public Summary summary(Long uid, int days) {
        ZoneId zone = ZoneId.of(props.zone());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate from = today.minusDays(days - 1L);

        List<TodoItem> todoList = todos.findByUserId(uid);
        List<Plan> planList = plans.findByUserId(uid);
        List<Goal> goalList = goals.findByUserId(uid);
        List<HabitLog> logList = habitLogs.findByUserIdAndLogDateBetween(uid, from, today);
        List<MoodEntry> moodList = moods.findByUserId(uid);

        // ----- per-day series -----
        List<DayPoint> series = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            final LocalDate day = d;
            int t = (int) todoList.stream().filter(x -> x.getCompletedAt() != null && x.getCompletedAt().toLocalDate().equals(day)).count();
            int p = (int) planList.stream().filter(x -> x.getCompletedAt() != null && x.getCompletedAt().toLocalDate().equals(day)).count();
            int h = (int) logList.stream().filter(x -> x.isSuccess() && x.getLogDate().equals(day)).count();
            Integer mood = moodList.stream().filter(x -> x.getEntryDate().equals(day)).map(MoodEntry::getScore).findFirst().orElse(null);
            series.add(new DayPoint(day, t, p, h, mood));
        }

        // ----- totals -----
        int todosDone = (int) todoList.stream().filter(TodoItem::isDone).count();
        int plansDone = (int) planList.stream().filter(Plan::isDone).count();
        int overdue = (int) planList.stream().filter(p -> !p.isDone() && p.getDeadline() != null && p.getDeadline().isBefore(now)).count();
        int goalsAvg = goalList.isEmpty() ? 0 : (int) Math.round(goalList.stream().mapToInt(Goal::getProgress).average().orElse(0));
        int habitRate = logList.isEmpty() ? 0 : Math.round(100f * logList.stream().filter(HabitLog::isSuccess).count() / logList.size());
        Double moodAvg = moodList.stream().filter(m -> !m.getEntryDate().isBefore(from)).mapToInt(MoodEntry::getScore).average().isPresent()
                ? Math.round(moodList.stream().filter(m -> !m.getEntryDate().isBefore(from)).mapToInt(MoodEntry::getScore).average().orElse(0) * 10) / 10.0
                : null;
        Totals totals = new Totals(todosDone, todoList.size() - todosDone, plansDone, planList.size() - plansDone,
                overdue, goalsAvg, habitRate, moodAvg);

        // ----- upcoming deadlines -----
        List<Upcoming> upcoming = planList.stream()
                .filter(p -> !p.isDone() && p.getDeadline() != null && !p.getDeadline().isBefore(now))
                .sorted(Comparator.comparing(Plan::getDeadline))
                .limit(5)
                .map(p -> new Upcoming(p.getId(), p.getTitle(), p.getDeadline()))
                .toList();

        // ----- insights (simple rules, they change as soon as the data changes) -----
        List<String> insights = new ArrayList<>();
        DayPoint todayPoint = series.get(series.size() - 1);
        int doneToday = todayPoint.todosCompleted() + todayPoint.plansCompleted() + todayPoint.habitsDone();
        if (doneToday == 0) {
            insights.add("Nothing ticked off yet today. Pick one small thing to start.");
        } else {
            insights.add("You have finished " + doneToday + (doneToday == 1 ? " thing" : " things") + " today.");
        }
        if (overdue > 0) {
            insights.add(overdue + (overdue == 1 ? " plan is" : " plans are") + " past the deadline. Finish it or move the date.");
        }
        long soon = planList.stream().filter(p -> !p.isDone() && p.getDeadline() != null
                && p.getDeadline().isAfter(now) && Duration.between(now, p.getDeadline()).toHours() < 48).count();
        if (soon > 0) {
            insights.add(soon + (soon == 1 ? " deadline is" : " deadlines are") + " due in the next 48 hours.");
        }
        if (moodAvg != null) {
            if (moodAvg >= 4) insights.add("Your mood has been bright lately (average " + moodAvg + " of 5).");
            else if (moodAvg <= 2.5) insights.add("Your mood has been low lately (average " + moodAvg + " of 5). A lighter plan might help.");
        }
        if (!goalList.isEmpty()) {
            goalList.stream().filter(g -> g.getProgress() < 100 && !g.getSteps().isEmpty())
                    .max(Comparator.comparingInt(Goal::getProgress))
                    .ifPresent(g -> insights.add("Closest goal: \"" + g.getTitle() + "\" at " + g.getProgress() + "%."));
        }
        if (!logList.isEmpty()) {
            insights.add("Habit success over the last " + days + " days: " + habitRate + "%.");
        }

        return new Summary(totals, series, insights, upcoming);
    }
}
