package com.mindfocus;

import com.mindfocus.domain.Goal;
import com.mindfocus.domain.GoalStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalProgressTest {
    private GoalStep step(int weight, boolean done) {
        GoalStep s = new GoalStep();
        s.setTitle("s");
        s.setWeight(weight);
        s.setDone(done);
        return s;
    }

    @Test
    void progressIsWeighted() {
        Goal g = new Goal();
        g.getSteps().add(step(1, true));
        g.getSteps().add(step(3, false));
        assertEquals(25, g.getProgress());
    }

    @Test
    void emptyGoalIsZero() {
        assertEquals(0, new Goal().getProgress());
    }
}
