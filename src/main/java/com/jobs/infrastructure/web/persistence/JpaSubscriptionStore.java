package com.jobs.infrastructure.web.persistence;

import com.jobs.application.port.SubscriptionStore;
import com.jobs.domain.Plan;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JpaSubscriptionStore implements SubscriptionStore {

    private final AccountJpaRepository repository;

    public JpaSubscriptionStore(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Plan getPlan(String username) {
        return repository.findById(username)
                .map(AccountEntity::getPlan)
                .filter(plan -> !plan.isBlank())
                .map(Plan::valueOf)
                .orElse(Plan.FREE);
    }

    @Override
    public Instant getLastSearchAt(String username) {
        return repository.findById(username)
                .map(AccountEntity::getLastSearchAt)
                .orElse(null);
    }

    @Override
    public void recordSearchNow(String username) {
        repository.findById(username).ifPresent(account -> {
            account.setLastSearchAt(Instant.now());
            repository.save(account);
        });
    }
}
