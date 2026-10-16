package com.mindfocus.web;

import com.mindfocus.repo.*;
import com.mindfocus.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Merges everything with a date into one feed for the calendar view. */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final PlanRepository plans;
    private final TodoRepository todos;
    private final GoalRepository goals;
    private final NoteRepository notes;

    public record CalendarEvent(String type, Long refId, String title, LocalDate date, LocalDateTime at, boolean done) {}

    @GetMapping
    public List<CalendarEvent> events(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long uid = CurrentUser.id();
        List<CalendarEvent> out = new ArrayList<>();
        plans.findByUserId(uid).forEach(p -> {
            LocalDate d = p.getDeadline() != null ? p.getDeadline().toLocalDate() : p.getPlanDate();
            if (within(d, from, to)) out.add(new CalendarEvent("plan", p.getId(), p.getTitle(), d, p.getDeadline(), p.isDone()));
        });
        todos.findByUserId(uid).forEach(t -> {
            if (t.getDueDate() != null && within(t.getDueDate(), from, to))
                out.add(new CalendarEvent("todo", t.getId(), t.getTitle(), t.getDueDate(), null, t.isDone()));
        });
        goals.findByUserId(uid).forEach(g -> {
            if (g.getTargetDate() != null && within(g.getTargetDate(), from, to))
                out.add(new CalendarEvent("goal", g.getId(), g.getTitle(), g.getTargetDate(), null, g.getProgress() == 100));
        });
        notes.findByUserId(uid).forEach(n -> {
            if (n.getNoteDate() != null && within(n.getNoteDate(), from, to))
                out.add(new CalendarEvent("note", n.getId(), n.getTitle(), n.getNoteDate(), null, false));
        });
        out.sort(Comparator.comparing(CalendarEvent::date));
        return out;
    }

    private boolean within(LocalDate d, LocalDate from, LocalDate to) {
        return d != null && !d.isBefore(from) && !d.isAfter(to);
    }
}
