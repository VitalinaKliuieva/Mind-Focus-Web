package com.mindfocus.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** One day of one habit. success = did it (BUILD) / stayed clean (QUIT). */
@Entity
@Table(name = "habit_logs", uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "log_date"}))
@Getter
@Setter
public class HabitLog extends OwnedEntity {
    @Column(name = "habit_id", nullable = false)
    private Long habitId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    private boolean success;
}
