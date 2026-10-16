package com.dayfolio.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note extends OwnedEntity {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate noteDate = LocalDate.now();

    private LocalDateTime createdAt = LocalDateTime.now();
}
