package com.dayfolio.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A day plan or month plan item, optionally with a deadline that triggers a reminder. */
@Entity
@Table(name = "plans")
@Getter
@Setter
public class Plan extends OwnedEntity {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PlanType type = PlanType.DAY;

    /** For DAY plans the day itself; for MONTH plans the first day of the month. */
    @NotNull
    private LocalDate planDate;

    private LocalDateTime deadline;

    private boolean done;

    private LocalDateTime completedAt;

    /** Minutes before the deadline when the reminder fires. */
    private Integer reminderMinutesBefore = 60;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean reminderSent;
}
