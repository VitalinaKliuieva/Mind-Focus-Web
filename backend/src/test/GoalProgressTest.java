package com.mindfocus;

import com.mindfocus.domain.Goal;
import com.mindfocus.domain.GoalStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit test: pure logic, no Spring, no database. Boundary values and rounding of the weighted progress. */
class GoalProgressTest {

    private static GoalStep step(int weight, boolean done) {
        GoalStep s = new GoalStep();
        s.setTitle("s");
        s.setWeight(weight);
        s.setDone(done);
        return s;
    }

    @ParameterizedTest(name = "weights [{0}], done mask {1} -> {2}%")
    @CsvSource(delimiter = '|', value = {
            "1,3|10|25",     // small step done
            "1,3|01|75",     // big step done
            "1,3|11|100",    // everything done
            "1,3|00|0",      // nothing done
            "1,2|10|33",     // 33.33 rounds down
            "1,2|01|67",     // 66.67 rounds up
            "1,1,1|110|67",  // two of three
            "100,1|01|1",    // 0.99 rounds up to 1
            "5|1|100"        // single step
    })
    @DisplayName("progress is weighted and rounded to a whole percent")
    void progressIsWeighted(String weights, String doneMask, int expected) {
        Goal g = new Goal();
        String[] w = weights.split(",");
        for (int i = 0; i < w.length; i++) {
            g.getSteps().add(step(Integer.parseInt(w[i]), doneMask.charAt(i) == '1'));
        }
        assertEquals(expected, g.getProgress());
    }

    @Test
    @DisplayName("a goal without steps has 0% and does not divide by zero")
    void emptyGoalIsZero() {
        assertEquals(0, new Goal().getProgress());
    }
}
