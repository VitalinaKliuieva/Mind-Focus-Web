package com.mindfocus.repo;

import com.mindfocus.domain.*;
import java.time.*;
import java.util.*;

public interface MoodRepository extends OwnedRepository<MoodEntry> {
    Optional<MoodEntry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);
}
