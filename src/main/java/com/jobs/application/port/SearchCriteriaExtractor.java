package com.jobs.application.port;

import com.jobs.domain.JobFilter;
import com.jobs.domain.UserProfile;

public interface SearchCriteriaExtractor {
    JobFilter extract(UserProfile profile) throws Exception;
}
