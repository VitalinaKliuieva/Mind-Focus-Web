package com.mindfocus.repo;

import com.mindfocus.domain.*;
import java.time.*;
import java.util.*;

public interface PlanRepository extends OwnedRepository<Plan> {
    List<Plan> findByDoneFalseAndReminderSentFalseAndDeadlineNotNullAndDeadlineBefore(LocalDateTime limit);
}
