package com.mindfocus.repo;

import com.mindfocus.domain.*;
import java.time.*;
import java.util.*;

public interface PushSubscriptionRepository extends OwnedRepository<PushSubscription> {
    boolean existsByEndpoint(String endpoint);
}
