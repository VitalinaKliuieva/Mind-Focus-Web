package com.mindfocus.repo;

import com.mindfocus.domain.*;
import java.time.*;
import java.util.*;

public interface HabitLogRepository extends OwnedRepository<HabitLog> {
    Optional<HabitLog> findByUserIdAndHabitIdAndLogDate(Long userId, Long habitId, LocalDate logDate);

    List<HabitLog> findByUserIdAndLogDateBetween(Long userId, LocalDate from, LocalDate to);

    void deleteByHabitId(Long habitId);
}
