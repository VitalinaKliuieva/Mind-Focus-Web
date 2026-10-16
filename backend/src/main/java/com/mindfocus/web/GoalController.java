package com.mindfocus.web;

import com.mindfocus.domain.Goal;
import com.mindfocus.domain.GoalStep;
import com.mindfocus.repo.GoalRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController extends OwnedCrudController<Goal> {
    public GoalController(GoalRepository repo) {
        super(repo);
    }

    @Override
    protected void copyInto(Goal t, Goal s) {
        t.setTitle(s.getTitle());
        t.setDescription(s.getDescription());
        t.setTargetDate(s.getTargetDate());
        // steps are replaced as a whole; orphanRemoval deletes the old rows
        t.getSteps().clear();
        for (GoalStep step : s.getSteps()) {
            step.setId(null);
            t.getSteps().add(step);
        }
    }

    @Override
    protected void prepare(Goal g) {
        for (GoalStep step : g.getSteps()) {
            step.setGoal(g);
        }
    }
}
