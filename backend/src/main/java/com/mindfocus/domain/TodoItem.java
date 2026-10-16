package com.dayfolio.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todo_items")
@Getter
@Setter
public class TodoItem extends OwnedEntity {
    @NotBlank
    @Size(max = 200)
    private String title;

    private boolean done;

    private LocalDate dueDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
}
