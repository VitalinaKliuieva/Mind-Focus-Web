package com.mindfocus.web;

import com.mindfocus.domain.Habit;
import com.mindfocus.domain.HabitLog;
import com.mindfocus.repo.HabitLogRepository;
import com.mindfocus.repo.HabitRepository;
import com.mindfocus.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/habits")
public class HabitController extends OwnedCrudController<Habit> {
    private final HabitLogRepository logs;

    public HabitController(HabitRepository repo, HabitLogRepository logs) {
        super(repo);
        this.logs = logs;
    }

    @Override
    protected void copyInto(Habit t, Habit s) {
        t.setName(s.getName());
        t.setKind(s.getKind());
        t.setStartDate(s.getStartDate());
    }

    @Override
    protected void beforeDelete(Habit h) {
        logs.deleteByHabitId(h.getId());
    }

    public record LogRequest(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, boolean success) {}

    public record HabitStat(Long habitId, int currentStreak, int bestStreak, int successRate30) {}

    @GetMapping("/logs")
    public List<HabitLog> logs(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return logs.findByUserIdAndLogDateBetween(CurrentUser.id(), from, to);
    }

    @PutMapping("/{id}/log")
    @Transactional
    public HabitLog setLog(@PathVariable Long id, @RequestBody LogRequest req) {
        find(id); // ownership check
        Long uid = CurrentUser.id();
        HabitLog log = logs.findByUserIdAndHabitIdAndLogDate(uid, id, req.date()).orElseGet(() -> {
            HabitLog l = new HabitLog();
            l.setUserId(uid);
            l.setHabitId(id);
            l.setLogDate(req.date());
            return l;
        });
        log.setSuccess(req.success());
        return logs.save(log);
    }

    @DeleteMapping("/{id}/log")
    @Transactional
    public void clearLog(@PathVariable Long id,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        find(id);
        logs.findByUserIdAndHabitIdAndLogDate(CurrentUser.id(), id, date).ifPresent(logs::delete);
    }

    @GetMapping("/stats")
    public List<HabitStat> stats() {
        Long uid = CurrentUser.id();
        LocalDate today = LocalDate.now();
        Map<Long, List<HabitLog>> byHabit = new HashMap<>();
        for (HabitLog l : logs.findByUserId(uid)) {
            byHabit.computeIfAbsent(l.getHabitId(), k -> new ArrayList<>()).add(l);
        }
        List<HabitStat> out = new ArrayList<>();
        for (Habit h : repo.findByUserId(uid)) {
            List<HabitLog> hl = byHabit.getOrDefault(h.getId(), List.of());
            Set<LocalDate> good = new HashSet<>();
            int recentTotal = 0, recentGood = 0;
            for (HabitLog l : hl) {
                if (l.isSuccess()) good.add(l.getLogDate());
                if (!l.getLogDate().isBefore(today.minusDays(29))) {
                    recentTotal++;
                    if (l.isSuccess()) recentGood++;
                }
            }
            // current streak: consecutive successful days ending today (or yesterday if today is not logged yet)
            LocalDate cursor = good.contains(today) ? today : today.minusDays(1);
            int current = 0;
            while (good.contains(cursor)) {
                current++;
                cursor = cursor.minusDays(1);
            }
            // best streak
            List<LocalDate> sorted = new ArrayList<>(good);
            Collections.sort(sorted);
            int best = 0, run = 0;
            LocalDate prev = null;
            for (LocalDate d : sorted) {
                run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
                best = Math.max(best, run);
                prev = d;
            }
            int rate = recentTotal == 0 ? 0 : Math.round(100f * recentGood / recentTotal);
            out.add(new HabitStat(h.getId(), current, best, rate));
        }
        return out;
    }
}
