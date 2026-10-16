package com.mindfocus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "habits")
@Getter
@Setter
public class Habit extends OwnedEntity {
    @NotBlank
    @Size(max = 120)
    private String name;

    /** BUILD = a habit to build, QUIT = a bad habit to quit. */
    @NotNull
    @Enumerated(EnumType.STRING)
    private HabitKind kind = HabitKind.BUILD;

    private LocalDate startDate = LocalDate.now();
}
