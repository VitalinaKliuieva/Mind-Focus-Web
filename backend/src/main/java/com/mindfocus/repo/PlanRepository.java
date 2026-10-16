package com.dayfolio.repo;

import com.dayfolio.domain.*;
import java.time.*;
import java.util.*;

public interface PlanRepository extends OwnedRepository<Plan> {
    List<Plan> findByDoneFalseAndReminderSentFalseAndDeadlineNotNullAndDeadlineBefore(LocalDateTime limit);
}
