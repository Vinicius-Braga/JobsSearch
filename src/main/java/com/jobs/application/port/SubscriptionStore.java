package com.jobs.application.port;

import com.jobs.domain.Plan;

import java.time.Instant;

public interface SubscriptionStore {
    Plan getPlan(String username);

    Instant getLastSearchAt(String username);

    void recordSearchNow(String username);
}
