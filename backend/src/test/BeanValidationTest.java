package com.mindfocus;

import com.mindfocus.domain.GoalStep;
import com.mindfocus.domain.Note;
import com.mindfocus.web.AuthController.RegisterRequest;
import com.mindfocus.web.MoodController.MoodRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Boundary value analysis of the validation rules, without starting Spring. */
class BeanValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "step weight {0} valid = {1}")
    @CsvSource({"-3,false", "0,false", "1,true", "50,true", "100,true", "101,false"})
    @DisplayName("goal step weight must be between 1 and 100")
    void stepWeightBounds(int weight, boolean valid) {
        GoalStep s = new GoalStep();
        s.setTitle("step");
        s.setWeight(weight);
        assertThat(VALIDATOR.validate(s).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest(name = "mood score {0} valid = {1}")
    @CsvSource({"-1,false", "0,false", "1,true", "3,true", "5,true", "6,false"})
    @DisplayName("mood score must be between 1 and 5")
    void moodScoreBounds(int score, boolean valid) {
        assertThat(VALIDATOR.validate(new MoodRequest(score, null)).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest(name = "title length {0} valid = {1}")
    @CsvSource({"1,true", "200,true", "201,false"})
    @DisplayName("note title is limited to 200 characters")
    void noteTitleLength(int length, boolean valid) {
        Note n = new Note();
        n.setTitle("x".repeat(length));
        assertThat(VALIDATOR.validate(n).isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest(name = "[{0}] is not a valid note title")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void blankNoteTitleIsRejected(String title) {
        Note n = new Note();
        n.setTitle(title);
        assertThat(VALIDATOR.validate(n)).isNotEmpty();
    }

    @ParameterizedTest(name = "{0} / {1} / {2}")
    @CsvSource(delimiter = '|', value = {
            "not-an-email|password1|Ann|false",
            "ann@example.com|1234567|Ann|false",     // 7 characters, one too short
            "ann@example.com|12345678|Ann|true",     // exactly 8 characters
            "ann@example.com|password1|''|false",    // blank display name
            "ann@example.com|password1|Ann|true"
    })
    @DisplayName("registration input rules")
    void registrationRules(String email, String password, String name, boolean valid) {
        assertThat(VALIDATOR.validate(new RegisterRequest(email, password, name)).isEmpty()).isEqualTo(valid);
    }
}
