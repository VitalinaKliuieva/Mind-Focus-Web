package com.mindfocus.web;

import com.mindfocus.domain.MoodEntry;
import com.mindfocus.repo.MoodRepository;
import com.mindfocus.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** One mood entry per day: PUT /api/moods/2026-10-05 creates or replaces it. */
@RestController
@RequestMapping("/api/moods")
@RequiredArgsConstructor
public class MoodController {
    private final MoodRepository moods;

    public record MoodRequest(@Min(1) @Max(5) int score, String note) {}

    @GetMapping
    public List<MoodEntry> list() {
        return moods.findByUserId(CurrentUser.id(), Sort.by(Sort.Direction.DESC, "entryDate"));
    }

    @PutMapping("/{date}")
    @Transactional
    public MoodEntry set(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @Valid @RequestBody MoodRequest req) {
        Long uid = CurrentUser.id();
        MoodEntry m = moods.findByUserIdAndEntryDate(uid, date).orElseGet(() -> {
            MoodEntry e = new MoodEntry();
            e.setUserId(uid);
            e.setEntryDate(date);
            return e;
        });
        m.setScore(req.score());
        m.setNote(req.note());
        return moods.save(m);
    }

    @DeleteMapping("/{date}")
    @Transactional
    public void delete(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        moods.findByUserIdAndEntryDate(CurrentUser.id(), date).ifPresent(moods::delete);
    }
}
