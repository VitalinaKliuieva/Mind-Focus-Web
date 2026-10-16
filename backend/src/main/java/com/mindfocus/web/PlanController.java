package com.mindfocus.web;

import com.mindfocus.domain.Plan;
import com.mindfocus.repo.PlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/api/plans")
public class PlanController extends OwnedCrudController<Plan> {
    public PlanController(PlanRepository repo) {
        super(repo);
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.ASC, "planDate", "deadline");
    }

    @Override
    protected void copyInto(Plan t, Plan s) {
        t.setTitle(s.getTitle());
        t.setDescription(s.getDescription());
        t.setType(s.getType());
        t.setPlanDate(s.getPlanDate());
        // a changed deadline or reminder offset re-arms the reminder
        if (!Objects.equals(t.getDeadline(), s.getDeadline())
                || !Objects.equals(t.getReminderMinutesBefore(), s.getReminderMinutesBefore())) {
            t.setReminderSent(false);
        }
        t.setDeadline(s.getDeadline());
        t.setReminderMinutesBefore(s.getReminderMinutesBefore());
        if (t.isDone() != s.isDone()) {
            t.setDone(s.isDone());
            t.setCompletedAt(s.isDone() ? LocalDateTime.now() : null);
        }
    }

    @Override
    protected void prepare(Plan p) {
        if (p.getReminderMinutesBefore() == null || p.getReminderMinutesBefore() < 0) {
            p.setReminderMinutesBefore(60);
        }
        if (p.isDone() && p.getCompletedAt() == null) {
            p.setCompletedAt(LocalDateTime.now());
        }
    }
}
