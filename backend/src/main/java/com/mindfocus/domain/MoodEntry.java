package com.mindfocus.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "mood_entries")
@Getter
@Setter
public class MoodEntry extends OwnedEntity {
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** 1 (rough) .. 5 (great) */
    private int score;

    @Column(columnDefinition = "TEXT")
    private String note;
}
