package com.jobs.infrastructure.web.persistence;

import com.jobs.application.port.ProfileStore;
import com.jobs.domain.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class JpaProfileStore implements ProfileStore {

    private final ProfileJpaRepository repository;

    public JpaProfileStore(ProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserProfile find(String username) {
        return repository.findById(username)
                .map(entity -> new UserProfile(entity.getDescription()))
                .orElse(null);
    }

    @Override
    public void save(String username, UserProfile profile) {
        repository.save(new ProfileEntity(username, profile.description()));
    }
}
