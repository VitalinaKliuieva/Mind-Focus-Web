package com.mindfocus.repo;

import com.mindfocus.domain.*;
import java.time.*;
import java.util.*;

public interface NotificationRepository extends OwnedRepository<AppNotification> {
    List<AppNotification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
