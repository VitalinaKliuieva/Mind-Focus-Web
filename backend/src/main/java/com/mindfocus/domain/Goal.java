package com.mindfocus.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goals")
@Getter
@Setter
public class Goal extends OwnedEntity {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate targetDate;

    @Valid
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<GoalStep> steps = new ArrayList<>();

    /** Percentage of done work, weighted by each step's weight (custom "scale"). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int getProgress() {
        int total = steps.stream().mapToInt(GoalStep::getWeight).sum();
        if (total == 0) return 0;
        int done = steps.stream().filter(GoalStep::isDone).mapToInt(GoalStep::getWeight).sum();
        return Math.round(100f * done / total);
    }
}
