package com.jobs.application.port;

import com.jobs.domain.UserProfile;

public interface ProfileStore {
    UserProfile find(String username);

    void save(String username, UserProfile profile);
}
